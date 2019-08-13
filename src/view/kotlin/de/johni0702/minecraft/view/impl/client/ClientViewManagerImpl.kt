package de.johni0702.minecraft.view.impl.client

import de.johni0702.minecraft.betterportals.common.popOrNull
import de.johni0702.minecraft.betterportals.common.pos
import de.johni0702.minecraft.betterportals.common.removeAtOrNull
import de.johni0702.minecraft.view.client.ClientView
import de.johni0702.minecraft.view.client.ClientViewManager
import de.johni0702.minecraft.view.impl.LOGGER
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
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import net.minecraftforge.fml.common.network.FMLNetworkEvent

internal class ClientViewManagerImpl : ClientViewManager {
    val mc: Minecraft = Minecraft.getMinecraft()

    override val player: EntityPlayerSP
        get() = mainView.clientPlayer

    override var mainView: ClientViewImpl = ClientViewImpl(this, 0, null, null, null, null).apply {
        captureState(mc)
    }

    private val unusedViews = mutableListOf<ClientViewImpl>()

    override val views = mutableListOf(mainView)

    override var activeView = mainView
    private var inUpdate = false

    override val serverMainView
        get() = unconfirmedChanges.firstOrNull()?.old ?: mainView

    /**
     * Queue containing the changes of main view which have not yet been confirmed by the server.
     * New changes are appended to the end. Whenever the server confirms an transaction, the first element is removed.
     */
    private val unconfirmedChanges = mutableListOf<MainViewChange>()

    fun getServerPlayer(view: ClientViewImpl): EntityPlayerSP {
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

    fun getClientPlayer(view: ClientViewImpl): EntityPlayerSP {
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
        mainView = ClientViewImpl(this, 0, null, null, null, null).also { it.captureState(mc) }
        activeView = mainView

        unusedViews.addAll(views)
        unusedViews.forEach { it.isValid = false }
        views.clear()
        views.add(mainView)
    }

    fun init() {
        MinecraftForge.EVENT_BUS.register(EventHandler())
    }

    fun createView(viewId: Int, world: WorldClient): ClientView? {
        if (views.find { it.id == viewId } != null) {
            throw IllegalArgumentException("View id $viewId is already taken")
        }
        return try {
            ClientViewImpl.reuseOrCreate(this, viewId, world, unusedViews.popOrNull()).also { views.add(it) }
        } catch (t: Throwable) {
            LOGGER.error("Creating world view:", t)
            views[viewId] = ClientViewImpl(this, viewId, null, null, null, null)
            null
        }
    }

    fun destroyView(view: ClientViewImpl) {
        LOGGER.debug("Removing view {}", view)
        if (activeView != mainView) throw IllegalStateException("Main view must be active")
        if (view == mainView) throw IllegalArgumentException("Cannot remove main view")

        withView(view) {
            mc.renderGlobal.setWorldAndLoadRenderers(null)
        }

        view.checkValid()
        check(views.remove(view)) { "Unknown view $view" }
        view.isValid = false
        unusedViews.add(view)
    }

    fun handleViewData(serverViewId: Int, data: ByteBuf) {
        try {
            val view = views.find { it.id == serverViewId }
            if (view == null) {
                LOGGER.warn("Received data for unknown view {}", serverViewId)
                return
            }
            val channel = view.channel
            if (channel != null) {
                updateView(view) {
                    data.retain()
                    channel.writeInbound(data)
                }
            } else {
                LOGGER.warn("Received data for main view {} via ViewData message", view)
            }
        } catch (t: Throwable) {
            LOGGER.error("Handling view data for view $serverViewId:", t)
        }
    }

    fun <T> updateView(view: ClientViewImpl, block: () -> T): T {
        if (inUpdate) {
            throw IllegalStateException("nested updateView")
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

    fun <T> withView(view: ClientViewImpl, block: () -> T): T {
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

    internal fun makeClientMainView(newMainView: ClientViewImpl) {
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
    }

    private fun makeMainView(newMainView: ClientViewImpl) {
        unconfirmedChanges.add(MainViewChange(mainView, newMainView, mainView.clientPlayer.pos))

        activeView.captureState(mc)
        newMainView.swapThePlayer(activeView, true)
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
    internal fun rewindMainView() {
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
        val badDims = listOf(
                "aether", // Aether 2
                "necromancertower", // Aether 2
                "aetheri" // Aether Legacy
        )
        if (mc.world.provider.dimensionType.name in badDims) {
            mc.soundHandler.stopSounds()
        }
    }

    fun makeMainViewAck(viewId: Int) {
        LOGGER.info("Ack for swap of {}", viewId)

        val expectedId = unconfirmedChanges.getOrNull(0)?.new?.id
        if (expectedId != viewId) {
            // We haven't requested this change, someone must have called `PlayerList.transferPlayerToDimension` on the server.
            // Rewind all view changes which haven't yet been confirmed by the server (it'll ignore them because it decided for us to go elsewhere)
            rewindMainView()
            // So, let's act as if we did request it
            makeMainView(views.find { it.id == viewId }!!)
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

        views.filter { !it.isMainView }.forEach { view ->
            withView(view) {
                tickView()
            }
        }

        mc.mcProfiler.endSection()
    }

    private fun tickView() {
        if (mc.entityRenderer == null) return

        mc.mcProfiler.startSection(activeView.id.toString())

        mc.entityRenderer.getMouseOver(1.0F)

        mc.mcProfiler.startSection("gameRenderer")

        mc.entityRenderer.updateRenderer()

        mc.mcProfiler.endStartSection("levelRenderer")

        mc.renderGlobal.updateClouds()

        mc.mcProfiler.endStartSection("level")

        if (mc.world.lastLightningBolt > 0) {
            mc.world.lastLightningBolt = mc.world.lastLightningBolt - 1
        }

        mc.world.updateEntities()

        mc.world.setAllowedSpawnTypes(mc.world.difficulty != EnumDifficulty.PEACEFUL, true)

        try {
            mc.world.tick()
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
        if (!mc.framebuffer.isStencilEnabled) {
            mc.framebuffer.enableStencil()
        }
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
        fun onDisconnect(event: FMLNetworkEvent.ClientDisconnectionFromServerEvent) {
            reset()
        }
    }

    interface IIntegratedServer {
        fun updateClientState(mc: Minecraft)
    }
}

internal data class MainViewChange(
        val old: ClientViewImpl,
        val new: ClientViewImpl,
        val fallbackPos: Vec3d
)