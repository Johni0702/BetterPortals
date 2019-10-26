package de.johni0702.minecraft.view.impl.client

import de.johni0702.minecraft.betterportals.common.DimensionId
import de.johni0702.minecraft.betterportals.common.forceAddEntity
import de.johni0702.minecraft.betterportals.common.forceRemoveEntity
import de.johni0702.minecraft.view.impl.LOGGER
import de.johni0702.minecraft.view.impl.net.CreateWorld
import io.netty.channel.embedded.EmbeddedChannel
import net.minecraft.client.Minecraft
import net.minecraft.client.entity.EntityPlayerSP
import net.minecraft.client.gui.GuiBossOverlay
import net.minecraft.client.multiplayer.WorldClient
import net.minecraft.client.particle.ParticleManager
import net.minecraft.client.renderer.EntityRenderer
import net.minecraft.client.renderer.ItemRenderer
import net.minecraft.client.renderer.RenderGlobal
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher
import net.minecraft.entity.Entity
import net.minecraft.network.*
import net.minecraft.util.math.RayTraceResult
import net.minecraft.world.WorldSettings

//#if MC>=11400
//#else
import net.minecraftforge.fml.common.network.handshake.NetworkDispatcher
//#endif

internal class ClientState(
        val manager: ClientWorldsManagerImpl,
        private var _world: WorldClient?,
        var thePlayer: EntityPlayerSP?,
        var channel: EmbeddedChannel?,
        var netManager: NetworkManager?
) {
    val isMainView: Boolean get() = manager.mainView == this
    val dimension: DimensionId get() = world.provider.dimension
    var isValid = true
    val player: EntityPlayerSP get() = manager.getServerPlayer(this)
    val clientPlayer: EntityPlayerSP get() = manager.getClientPlayer(this)

    val world: WorldClient
        get() = if (manager.activeView == this) {
            Minecraft.getMinecraft().world
        } else {
            _world!!
        }

    /**
     * Swaps thePlayer entities with the given view.
     * **Warning:** Must be followed by restoreView as otherwise Minecraft's state will be invalid!
     */
    internal fun swapThePlayer(with: ClientState, swapPos: Boolean) {
        val mc = Minecraft.getMinecraft()

        val thisPlayer = this.thePlayer!!
        val withPlayer = with.thePlayer!!
        val mcPlayer = if (mc.player == thisPlayer) withPlayer else thisPlayer

        this.world.forceRemoveEntity(thisPlayer)
        with.world.forceRemoveEntity(withPlayer)

        // We need to set thePlayer to null because it'll be used for the getEntityByID lookup in displaceEntity
        this.thePlayer = null
        with.thePlayer = null
        mc.player = null

        this.reintroduceDisplacedEntity()
        with.reintroduceDisplacedEntity()

        this.displaceEntity(withPlayer.entityId)
        with.displaceEntity(thisPlayer.entityId)

        thisPlayer.isDead = false
        withPlayer.isDead = false

        thisPlayer.setWorld(with.world)
        withPlayer.setWorld(this.world)

        thisPlayer.dimension = withPlayer.dimension.also { withPlayer.dimension = thisPlayer.dimension }

        if (swapPos) {
            thisPlayer.swapClientPosRotWith(withPlayer)
        }

        if (this.renderViewEntity == thisPlayer) {
            this.renderViewEntity = withPlayer
        }
        if (with.renderViewEntity == withPlayer) {
            with.renderViewEntity = thisPlayer
        }

        this.thePlayer = withPlayer
        with.thePlayer = thisPlayer

        // Some mods react to the entity spawn event fired when the player is added to the world
        // E.g. StorageDrawers (#304), DynamicSurrounds (#313)
        mc.player = mcPlayer

        this.world.forceAddEntity(withPlayer)
        with.world.forceAddEntity(thisPlayer)
    }

    /**
     * If an entity with the same id as the client player entity needs to be temporarily removed from this world
     * when the client player switches to it, then it's stored here to be re-added during packet handling.
     */
    private var displacedEntity: Entity? = null

    private fun displaceEntity(entityId: Int) {
        val world = _world ?: return
        world.getEntityByID(entityId)?.let {
            world.forceRemoveEntity(it)
            it.isDead = false
            displacedEntity = it
        }
    }

    private fun reintroduceDisplacedEntity() {
        displacedEntity?.let {
            displacedEntity = null

            val world = _world ?: return
            it.setWorld(world)
            it.dimension = world.provider.dimension
            world.forceAddEntity(it)
        }
    }

    private var renderViewEntity: Entity? = null
    private var itemRenderer: ItemRenderer? = null
    var renderGlobal: RenderGlobal? = null
    private var entityRenderer: EntityRenderer? = null
    private var particleManager: ParticleManager? = null
    private var pointedEntity: Entity? = null
    private var objectMouseOver: RayTraceResult? = null
    private var guiBossOverlay: GuiBossOverlay? = null

    override fun toString(): String = "ClientState for world ${world.provider.dimension}"

    internal fun captureState(mc: Minecraft) {
        itemRenderer = mc.itemRenderer
        particleManager = mc.effectRenderer
        renderGlobal = mc.renderGlobal
        entityRenderer = mc.entityRenderer
        _world = mc.world
        thePlayer = mc.player
        renderViewEntity = mc.renderViewEntity
        netManager = mc.connection?.netManager
        pointedEntity = mc.pointedEntity
        objectMouseOver = mc.objectMouseOver
        guiBossOverlay = mc.ingameGUI?.bossOverlay
    }

    internal fun restoreState(mc: Minecraft) {
        mc.itemRenderer = itemRenderer
        mc.effectRenderer = particleManager
        mc.renderGlobal = renderGlobal
        mc.entityRenderer = entityRenderer
        mc.player = thePlayer
        mc.world = _world
        val connection = mc.connection
        if (connection != null) {
            connection.netManager = netManager?.apply { netHandler = connection }
            connection.clientWorldController = mc.world
        }
        TileEntityRendererDispatcher.instance.setWorld(mc.world)

        mc.pointedEntity = pointedEntity
        mc.objectMouseOver = objectMouseOver
        if (mc.ingameGUI != null && guiBossOverlay != null) {
            mc.ingameGUI.overlayBoss = guiBossOverlay
        }

        if (mc.entityRenderer != null) {
            mc.renderViewEntity = renderViewEntity
        }
    }

    internal fun copyRenderState(from: ClientState) {
        entityRenderer!!.fovModifierHand = from.entityRenderer!!.fovModifierHand
        entityRenderer!!.fovModifierHandPrev = from.entityRenderer!!.fovModifierHandPrev
        itemRenderer!!.itemStackMainHand = from.itemRenderer!!.itemStackMainHand
        itemRenderer!!.itemStackOffHand = from.itemRenderer!!.itemStackOffHand
        itemRenderer!!.equippedProgressMainHand = from.itemRenderer!!.equippedProgressMainHand
        itemRenderer!!.prevEquippedProgressMainHand = from.itemRenderer!!.prevEquippedProgressMainHand
        itemRenderer!!.equippedProgressOffHand = from.itemRenderer!!.equippedProgressOffHand
        itemRenderer!!.prevEquippedProgressOffHand = from.itemRenderer!!.prevEquippedProgressOffHand
    }

    companion object {
        fun reuseOrCreate(manager: ClientWorldsManagerImpl, message: CreateWorld, oldView: ClientState?): ClientState {
            val mc = manager.mc
            val connection = mc.connection ?: throw IllegalStateException("Cannot create view without active connection")
            val networkManager = ViewNetworkManager()
            networkManager.netHandler = connection
            val channel = EmbeddedChannel()
            channel.pipeline()
                    .addLast("splitter", NettyVarint21FrameDecoder())
                    .addLast("decoder", NettyPacketDecoder(EnumPacketDirection.CLIENTBOUND))
                    .addLast("packet_handler", networkManager)
                    .fireChannelActive()
            networkManager.setConnectionState(EnumConnectionState.PLAY)

            //#if MC>=11400
            //$$ // FIXME oh no, I gotta understand forge's networking mess again
            //#else
            val networkDispatcher = NetworkDispatcher.allocAndSet(networkManager)
            channel.pipeline().addBefore("packet_handler", "fml:packet_handler", networkDispatcher)

            val stateField = NetworkDispatcher::class.java.getDeclaredField("state")
            val connectedState = stateField.type.asSubclass(Enum::class.java).enumConstants.last()
            stateField.isAccessible = true
            stateField.set(networkDispatcher, connectedState)
            //#endif

            val view: ClientState
            if (oldView == null) {
                LOGGER.debug("Creating new view")
                view = ClientState(manager, null, null, channel, networkManager)

            } else {
                LOGGER.debug("Reusing stored view")
                view = ClientState(manager, null, null, channel, networkManager)
                view.itemRenderer = oldView.itemRenderer
                view.renderGlobal = oldView.renderGlobal
                view.entityRenderer = oldView.entityRenderer
                view.particleManager = oldView.particleManager
            }

            //#if MC>=11400
            //$$ val orgParticleManager = mc.particles as IParticleManager
            //#endif

            manager.withView(view) {
                if (view.itemRenderer == null) {
                    // Need to initialize the newly create view state while it's active since several of the components
                    // get their own dependencies implicitly via the Minecraft instance
                    view.itemRenderer = ItemRenderer(mc)
                    mc.itemRenderer = view.itemRenderer // Implicitly passed to EntityRenderer via mc
                    view.renderGlobal = RenderGlobal(mc)
                    mc.renderGlobal = view.renderGlobal
                    view.entityRenderer = EntityRenderer(mc, mc.resourceManager)
                    mc.entityRenderer = view.entityRenderer
                    //#if MC>=11400
                    //$$ view.particleManager = orgParticleManager.createWithSharedAtlas()
                    //#else
                    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS") // incorrect default annotation
                    view.particleManager = ParticleManager(null, mc.textureManager)
                    //#endif
                    mc.effectRenderer = view.particleManager
                }
            }

            val world = WorldClient(mc.connection!!,
                    WorldSettings(0L,
                            message.gameType!!,
                            false,
                            mc.world.worldInfo.isHardcoreModeEnabled,
                            message.worldType!!),
                    message.dimensionID,
                    //#if MC>=11400
                    //$$ 0,
                    //$$ mc.profiler,
                    //$$ mc.worldRenderer
                    //#else
                    message.difficulty!!,
                    mc.mcProfiler
                    //#endif
            )
            val camera = ViewEntity(world, connection)

            view.netManager = networkManager
            view._world = world
            view.thePlayer = camera
            world.forceAddEntity(camera)

            manager.withView(view) {
                with (mc.renderGlobal) {
                    if (renderDispatcher != null) {
                        renderDispatcher.stopWorkerThreads()
                    }
                    renderDispatcher = manager.mainView.renderGlobal!!.renderDispatcher
                    setWorldAndLoadRenderers(world)
                }
                mc.effectRenderer.clearEffects(world)
                mc.ingameGUI.overlayBoss = GuiBossOverlay(mc)
            }

            return view
        }
    }

    //#if MC>=11400
    //$$ interface IParticleManager {
    //$$     fun createWithSharedAtlas(): ParticleManager
    //$$ }
    //#endif
}
