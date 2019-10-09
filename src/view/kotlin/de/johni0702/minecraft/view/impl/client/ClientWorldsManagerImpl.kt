package de.johni0702.minecraft.view.impl.client

import de.johni0702.minecraft.betterportals.common.DimensionId
import de.johni0702.minecraft.betterportals.common.popOrNull
import de.johni0702.minecraft.betterportals.common.pos
import de.johni0702.minecraft.betterportals.common.removeAtOrNull
import de.johni0702.minecraft.view.client.ClientWorldsManager
import de.johni0702.minecraft.view.impl.LOGGER
import de.johni0702.minecraft.view.impl.net.CreateWorld
import io.netty.buffer.ByteBuf
import net.minecraft.client.Minecraft
import net.minecraft.client.entity.EntityPlayerSP
import net.minecraft.client.multiplayer.WorldClient
import net.minecraft.crash.CrashReport
import net.minecraft.network.play.client.CPacketPlayer
import net.minecraft.util.ReportedException
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.Vec3d
import net.minecraft.world.EnumDifficulty
import net.minecraftforge.client.event.RenderGameOverlayEvent
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent

//#if MC>=11400
//$$ import net.minecraft.util.ResourceLocation
//$$ import net.minecraftforge.event.entity.player.PlayerEvent
//#else
import net.minecraftforge.fml.common.network.FMLNetworkEvent
//#endif

internal class ClientWorldsManagerImpl : ClientWorldsManager {
    override val worlds: List<WorldClient>
        get() = views.map { it.world }

    override fun changeDimension(newWorld: WorldClient, updatePosition: EntityPlayerSP.() -> Unit) {
        val view = views.find { it.world == newWorld }
        check(view != null) { "Unknown world $newWorld" }
        makeClientMainView(view, updatePosition)
    }

    val mc: Minecraft = Minecraft.getMinecraft()

    override val player: EntityPlayerSP
        get() = mainView.clientPlayer

    var mainView: ClientState = ClientState(this, null, null, null, null).apply {
        captureState(mc)
    }

    private val unusedViews = mutableListOf<ClientState>()

    val views = mutableListOf(mainView)

    var activeView = mainView
    private var inUpdate = false

    val serverMainView
        get() = unconfirmedChanges.firstOrNull()?.old ?: mainView

    /**
     * Queue containing the changes of main view which have not yet been confirmed by the server.
     * New changes are appended to the end. Whenever the server confirms an transaction, the first element is removed.
     */
    private val unconfirmedChanges = mutableListOf<MainViewChange>()

    fun getServerPlayer(view: ClientState): EntityPlayerSP {
        val serverView = if (inUpdate) {
            view
        } else {
            unconfirmedChanges.asReversed().fold(view) { curr, change ->
                if (change.new == curr) {
                    change.old
                } else {
                    curr
                }
            }
        }
        return if (activeView == serverView) {
            mc.player
        } else {
            serverView.thePlayer!!
        }
    }

    fun getClientPlayer(view: ClientState): EntityPlayerSP {
        val clientView = if (inUpdate) {
            unconfirmedChanges.fold(view) { curr, change ->
                if (change.old == curr) {
                    change.new
                } else {
                    curr
                }
            }
        } else {
            view
        }
        return if (activeView == clientView) {
            mc.player
        } else {
            clientView.thePlayer!!
        }
    }

    private fun reset() {
        unconfirmedChanges.clear()

        views.remove(mainView)
        mainView = ClientState(this, null, null, null, null).also { it.captureState(mc) }
        activeView = mainView

        unusedViews.addAll(views)
        unusedViews.forEach { it.isValid = false }
        views.clear()
        views.add(mainView)
    }

    fun init() {
        MinecraftForge.EVENT_BUS.register(EventHandler())
    }

    fun createState(message: CreateWorld): ClientState {
        val dim = message.dimensionID
        check(views.find { it.world.provider.dimension == dim } == null) { "World with dimension $dim already exists" }
        return ClientState.reuseOrCreate(this, message, unusedViews.popOrNull()).also { views.add(it) }
    }

    fun destroyState(view: ClientState) {
        LOGGER.debug("Removing view {}", view)
        if (activeView != mainView) throw IllegalStateException("Main view must be active")
        if (view == mainView) throw IllegalArgumentException("Cannot remove main view")

        withView(view) {
            mc.renderGlobal.setWorldAndLoadRenderers(null)
        }

        check(views.remove(view)) { "Unknown view $view" }
        view.isValid = false
        unusedViews.add(view)
    }

    fun handleWorldData(dimensionId: DimensionId, data: ByteBuf) {
        try {
            val view = views.find { it.dimension == dimensionId }
            if (view == null) {
                LOGGER.warn("Received data for unknown dimension {}", dimensionId)
                return
            }
            val channel = view.channel
            if (channel != null) {
                updateState(view) {
                    data.retain()
                    channel.writeInbound(data)
                }
            } else {
                LOGGER.warn("Received data for main dimension {} via WorldData message", view)
            }
        } catch (t: Throwable) {
            LOGGER.error("Handling view data for dimension $dimensionId:", t)
        }
    }

    fun <T> updateState(view: ClientState, block: () -> T): T {
        if (inUpdate) {
            throw IllegalStateException("nested updateState")
        }
        if (activeView != mainView) {
            throw IllegalStateException("already in withView")
        }

        // Reverse any unconfirmed client changes and switch to view
        mainView.captureState(mc)
        unconfirmedChanges.asReversed().forEach { change ->
            change.old.swapThePlayer(change.new, false)
        }
        activeView = view
        activeView.restoreState(mc)

        inUpdate = true
        try {
            // Run updates
            return block()
        } finally {
            inUpdate = false

            // Re-apply any remaining unconfirmed client changes and switch back
            activeView.captureState(mc)
            unconfirmedChanges.forEach { change ->
                change.old.swapThePlayer(change.new, false)
            }
            activeView = mainView
            activeView.restoreState(mc)
        }
    }

    fun <T> withView(view: ClientState, block: () -> T): T {
        if (activeView != mainView) {
            throw IllegalStateException("already in withView")
        }
        if (view == mainView) {
            return block()
        }
        activeView.captureState(mc)
        activeView = view
        activeView.restoreState(mc)
        try {
            return block()
        } finally {
            activeView.captureState(mc)
            activeView = mainView
            activeView.restoreState(mc)
        }
    }

    internal fun makeClientMainView(newMainView: ClientState, updatePosition: EntityPlayerSP.() -> Unit) {
        if (inUpdate) {
            throw IllegalStateException("Cannot change main view during update / packet handling")
        }
        if (activeView != mainView) {
            throw IllegalStateException("Needs to be called with the current main view active")
        }

        with(player) {
            connection.sendPacket(CPacketPlayer.PositionRotation(
                    posX, entityBoundingBox.minY, posZ, rotationYaw, rotationPitch, onGround))
        }

        LOGGER.info("Swapping main view $mainView with $newMainView")

        makeMainView(newMainView)
        updatePosition(player)
    }

    private fun makeMainView(newMainView: ClientState) {
        unconfirmedChanges.add(MainViewChange(mainView, newMainView, mainView.clientPlayer.pos))

        activeView.captureState(mc)
        newMainView.swapThePlayer(activeView, false)
        newMainView.copyRenderState(activeView)
        newMainView.restoreState(mc)
        activeView = newMainView
        mainView = newMainView

        stopMusic()
    }

    /**
     * Rewinds all changes of main view which haven't been confirmed by the server.
     * Must only be called from main view during update (i.e. caused by a teleport packet sent from the server).
     */
    fun rewindMainView() {
        val fallbackPos = unconfirmedChanges.firstOrNull()?.fallbackPos ?: return

        LOGGER.warn("Got teleport in old main view, rewinding main view changes to before that change..")

        if (!inUpdate) {
            throw IllegalStateException("rewind outside update")
        }
        if (activeView != serverMainView) {
            throw IllegalStateException("rewind outside server main view")
        }

        // Since we're inUpdate (where all state is already the server state), rewinding the changes is trivial
        unconfirmedChanges.clear()
        mainView = activeView
        with(fallbackPos) { player.setPosition(x, y, z) }

        stopMusic()
    }

    private fun stopMusic() {
        // Certain mods (e.g. Aether Legacy and Aether II) have their own music manager which relies on the
        // world change to cancel any previous music (because changing worlds usually stops all sounds).
        // There isn't any good way to detect these mods and always stopping music would be wasteful, so instead
        // we're having a dimension type blacklist
        //#if MC>=11400
        //$$ val badDims = listOf<ResourceLocation>(
        //$$ )
        //$$ if (mc.world.dimension.type.registryName in badDims) {
        //$$     mc.soundHandler.stop()
        //$$ }
        //#else
        val badDims = listOf(
                "aether", // Aether 2
                "necromancertower", // Aether 2
                "aetheri" // Aether Legacy
        )
        if (mc.world.provider.dimensionType.name in badDims) {
            mc.soundHandler.stopSounds()
        }
        //#endif
    }

    fun makeMainViewAck(dimensionId: DimensionId) {
        LOGGER.info("Ack for swap of {}", dimensionId)

        val expectedId = unconfirmedChanges.getOrNull(0)?.new?.dimension
        if (expectedId != dimensionId) {
            // We haven't requested this change, someone must have called `PlayerList.transferPlayerToDimension` on the server.
            // Rewind all view changes which haven't yet been confirmed by the server (it'll ignore them because it decided for us to go elsewhere)
            rewindMainView()
            // So, let's act as if we did request it
            makeMainView(views.find { it.dimension == dimensionId }!!)
        }

        val change = unconfirmedChanges.removeAtOrNull(0)!!
        val newMainView = change.new
        val oldMainView = change.old

        activeView.captureState(mc)

        oldMainView.channel = newMainView.channel.also { newMainView.channel = oldMainView.channel }
        oldMainView.netManager = newMainView.netManager.also { newMainView.netManager = oldMainView.netManager }

        activeView.restoreState(mc)
    }

    private fun tickViews() {
        if (mc.isGamePaused) {
            return
        }

        (mc.integratedServer as? IIntegratedServer)?.updateClientState(mc)

        mc.mcProfiler.startSection("tickViews")

        views.filter { it != mainView }.forEach { view ->
            withView(view) {
                tickView()
            }
        }

        mc.mcProfiler.endSection()
    }

    private fun tickView() {
        if (mc.entityRenderer == null) return

        mc.mcProfiler.startSection(activeView.dimension.toString())

        mc.entityRenderer.getMouseOver(1.0F)

        mc.mcProfiler.startSection("gameRenderer")

        mc.entityRenderer.updateRenderer()

        mc.mcProfiler.endStartSection("levelRenderer")

        mc.renderGlobal.updateClouds()

        mc.mcProfiler.endStartSection("level")

        if (mc.world.lastLightningBolt > 0) {
            mc.world.lastLightningBolt = mc.world.lastLightningBolt - 1
        }

        //#if MC>=11400
        //$$ mc.world.tickEntities()
        //#else
        mc.world.updateEntities()
        //#endif

        mc.world.setAllowedSpawnTypes(mc.world.difficulty != EnumDifficulty.PEACEFUL, true)

        try {
            //#if MC>=11400
            //$$ mc.world.tick { true }
            //#else
            mc.world.tick()
            //#endif
        } catch (t: Throwable) {
            val crash = CrashReport.makeCrashReport(t, "Exception in world tick")
            mc.world.addWorldInfoToCrashReport(crash)
            throw ReportedException(crash)
        }

        mc.mcProfiler.endStartSection("animateTick")

        mc.world.doVoidFogParticles(MathHelper.floor(mc.player.posX), MathHelper.floor(mc.player.posY), MathHelper.floor(mc.player.posZ))

        mc.mcProfiler.endStartSection("particles")

        mc.effectRenderer.updateEffects()

        mc.mcProfiler.endSection()
        mc.mcProfiler.endSection()
    }

    private fun preRender() {
        // Make sure the stencil buffer is enabled
        //#if MC>=11400
        //$$ // TODO we don't actually use the stencil buffer atm. if we do, this needs porting
        //#else
        if (!mc.framebuffer.isStencilEnabled) {
            mc.framebuffer.enableStencil()
        }
        //#endif
    }

    private inner class EventHandler {

        @SubscribeEvent(priority = EventPriority.LOWEST)
        fun postClientTick(event: TickEvent.ClientTickEvent) {
            if (event.phase != TickEvent.Phase.END) return

            tickViews()
        }

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        fun preClientRender(event: TickEvent.RenderTickEvent) {
            if (event.phase != TickEvent.Phase.START) return

            preRender()
        }

        @SubscribeEvent(priority = EventPriority.LOW)
        fun onDisconnect(
                //#if MC>=11400
                //$$ event: PlayerEvent.PlayerLoggedOutEvent
                //#else
                event: FMLNetworkEvent.ClientDisconnectionFromServerEvent
                //#endif
        ) {
            reset()
        }

        @SubscribeEvent
        fun addWorldsDebugInfo(event: RenderGameOverlayEvent.Text) {
            val list = event.left
            list.indexOfFirst { it.startsWith("E: ") }.let { idx ->
                if (idx == -1) return@let
                list.removeAt(idx)
                views.forEach {
                    list.add(idx, "Dim: ${it.world.provider.dimension}, ${it.renderGlobal?.debugInfoEntities}")
                }
            }
            list.indexOfFirst {
                //#if MC>=11400
                //$$ it.startsWith("Client Chunk Cache: ")
                //#else
                it.startsWith("MultiplayerChunkCache: ")
                //#endif
            }.let { idx ->
                if (idx == -1) return@let
                list.removeAt(idx)
                worlds.forEach {
                    list.add(idx, "Dim: ${it.provider.dimension}, ${it.providerName}")
                }
            }
        }
    }

    interface IIntegratedServer {
        fun updateClientState(mc: Minecraft)
    }
}

internal data class MainViewChange(
        val old: ClientState,
        val new: ClientState,
        val fallbackPos: Vec3d
)