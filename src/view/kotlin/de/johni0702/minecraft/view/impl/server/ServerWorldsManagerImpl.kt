package de.johni0702.minecraft.view.impl.server

import com.mojang.authlib.GameProfile
import de.johni0702.minecraft.betterportals.common.*
import de.johni0702.minecraft.view.impl.MOD_ID
import de.johni0702.minecraft.view.impl.net.*
import de.johni0702.minecraft.view.server.CubeSelector
import de.johni0702.minecraft.view.server.CuboidCubeSelector
import de.johni0702.minecraft.view.server.ServerWorldsManager
import de.johni0702.minecraft.view.server.View
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.channel.embedded.EmbeddedChannel
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.network.*
import net.minecraft.network.play.server.SPacketCustomPayload
import net.minecraft.network.play.server.SPacketDestroyEntities
import net.minecraft.server.MinecraftServer
import net.minecraft.util.math.ChunkPos
import net.minecraft.util.math.Vec3d
import net.minecraft.util.math.Vec3i
import net.minecraft.world.WorldServer
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.entity.player.PlayerEvent
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import net.minecraftforge.fml.common.network.handshake.NetworkDispatcher
import java.util.*

internal class ServerWorldsManagerImpl(
        val server: MinecraftServer,
        val connection: NetHandlerPlayServer
) : ServerWorldsManager {
    val worldManagers = mutableMapOf<WorldServer, ServerWorldManager>()
    init {
        val world = connection.player.serverWorld
        worldManagers[world] = ServerWorldManager(this, world, connection.player)
    }

    val mainWorldManager
        get() = worldManagers[connection.player.world] ?: throw IllegalStateException("Missing world manager for ${connection.player.world}")

    override val views
        get() = worldManagers.mapValues { it.value.views }

    override fun createView(world: WorldServer, pos: Vec3d, anchor: Pair<WorldServer, Vec3i>?): View = object : View {
        override var isValid: Boolean = true
        override fun dispose() {
            isValid = false
        }
        override val manager: ServerWorldsManager = this@ServerWorldsManagerImpl
        override val world: WorldServer = world
        override val center: Vec3d = pos
        // TODO somehow update when render distance changes
        override val cubeSelector: CubeSelector = player.server!!.playerList.let { playerList ->
            CuboidCubeSelector(
                    pos.toBlockPos().toCubePos(),
                    playerList.viewDistance,
                    if (world.isCubicWorld) playerList.verticalViewDistance else playerList.viewDistance
            )
        }
        override val anchor: Pair<WorldServer, Vec3i>? = anchor
    }.also { registerView(it) }

    override fun registerView(view: View) {
        getOrCreateWorldManager(view.world).views.add(view)
        updateActiveViews()
    }

    fun updateActiveViews() {
        determineActiveViews().forEach { (world, tracked) ->
            val manager = worldManagers.getValue(world)
            if (manager.activeViews.toSet() != tracked) {
                manager.activeViews = tracked
                manager.needsUpdate = true
            }
        }
    }

    private fun determineActiveViews(): Map<WorldServer, MutableList<View>> {
        val trackedViews = worldManagers.mapValues { mutableListOf<View>() }
        val queuedViews = mutableListOf<View>()
        if (!player.hasDisconnected()) {
            queuedViews.add(VanillaView(this, player))
        }
        worldManagers.values.forEach { queuedViews.addAll(it.views) }

        while (true) {
            var changed = false
            val iter = queuedViews.iterator()
            while (iter.hasNext()) {
                val view = iter.next()
                val anchor = view.anchor
                val included = if (anchor != null) {
                    val anchorWorld = anchor.first
                    val anchorPos = anchor.second
                    val anchorChunkPos = ChunkPos(anchorPos.x, anchorPos.z)
                    trackedViews[anchorWorld]?.any {
                        if (anchorWorld.isCubicWorld) {
                            it.cubeSelector.isCubeIncluded(anchorPos)
                        } else {
                            it.cubeSelector.isColumnIncluded(anchorChunkPos)
                        }
                    } == true
                } else {
                    true
                }
                if (included) {
                    trackedViews.getValue(view.world).add(view)
                    iter.remove()
                    changed = true
                }
            }
            if (!changed) {
                break
            }
        }

        return trackedViews
    }

    override val worlds: List<WorldServer>
        get() = worldManagers.keys.toList()

    override fun changeDimension(newWorld: WorldServer, updatePosition: EntityPlayerMP.() -> Unit) {
        getOrCreateWorldManager(newWorld).makeMainWorld(updatePosition)
    }

    override var player: EntityPlayerMP = connection.player

    private val eventHandler = EventHandler()

    init {
        eventHandler.registered = true
    }

    private fun getOrCreateWorldManager(world: WorldServer): ServerWorldManager =
            worldManagers[world] ?: createWorldManager(world)

    private fun createWorldManager(world: WorldServer): ServerWorldManager {
        val channel = EmbeddedChannel()
        val gameProfile = GameProfile(UUID.randomUUID(), connection.player.name + "[view]")
        val camera = ViewEntity(world, gameProfile, connection, channel)

        channel.pipeline()
                .addLast("prepender", NettyVarint21FrameEncoder())
                .addLast("encoder", NettyPacketEncoder(EnumPacketDirection.CLIENTBOUND))
                .addLast("exception_handler", NettyExceptionHandler(connection))
                .addLast("packet_handler", camera.connection.networkManager)
                .fireChannelActive()
        camera.connection.networkManager.setConnectionState(EnumConnectionState.PLAY)

        val networkDispatcher = NetworkDispatcher.allocAndSet(camera.connection.networkManager, server.playerList)
        channel.pipeline().addBefore("packet_handler", "fml:packet_handler", networkDispatcher)

        val stateField = NetworkDispatcher::class.java.getDeclaredField("state")
        val connectedState = stateField.type.asSubclass(Enum::class.java).enumConstants.last()
        stateField.isAccessible = true
        stateField.set(networkDispatcher, connectedState)

        val worldManager = ServerWorldManager(this, world, camera)
        worldManagers[world] = worldManager

        CreateWorld(camera.dimension, world.difficulty,
                world.worldInfo.gameType, world.worldType).sendTo(connection.player)
        world.spawnEntity(camera)
        server.playerList.preparePlayer(camera, null)
        server.playerList.updateTimeAndWeatherForPlayer(camera, world)
        camera.connection.setPlayerLocation(camera.posX, camera.posY, camera.posZ, camera.rotationYaw, camera.rotationPitch)

        // Ensure the view entity and world is synced to the client
        flushPackets()
        return worldManager
    }

    private fun destroyWorldManager(manager: ServerWorldManager) {
        if (!connection.netManager.isChannelOpen) return

        // Flush packets before actually removing the world,
        // otherwise entities referencing the world (e.g. portals) might not yet have been removed on the client
        flushPackets()

        DestroyWorld(manager.world.provider.dimension).sendTo(connection.player)

        val player = manager.player
        val world = manager.world
        world.removeEntity(player)
        world.playerChunkMap.removePlayer(player)

        check(worldManagers.remove(manager.world, manager)) { "unknown manager $manager" }
    }

    private fun destroy() {
        eventHandler.registered = false

        worldManagers.toList().forEach { (world, manager) ->
            val player = manager.player as? ViewEntity ?: return@forEach
            world.removeEntity(player)
            world.playerChunkMap.removePlayer(player)
            worldManagers.remove(world)
        }
    }

    private fun tick() {
        worldManagers.values.forEach { manager ->
            manager.views.removeIf { !it.isValid }
            if (manager.activeViews.removeIf { !it.isValid }) {
                manager.needsUpdate = true
            }
            if (manager.needsUpdate) {
                manager.world.playerChunkMap.updateMovingPlayer(manager.player)
            }
        }
        worldManagers.values.filter { it.views.isEmpty() && it.player is ViewEntity }.forEach {
            destroyWorldManager(it)
        }

        flushPackets()
    }

    override fun sendPacket(world: WorldServer, packet: Packet<*>) {
        worldManagers[world]?.player?.connection?.sendPacket(packet)
    }

    override fun flushPackets() {
        // For some reason MC queues up removed entity ids instead of sending them directly (maybe to save packets?).
        // Anyhow, we need them sent out right now.
        val flushEntityPackets = { player: EntityPlayerMP ->
            if (player.entityRemoveQueue.isNotEmpty()) {
                player.connection.sendPacket(SPacketDestroyEntities(*(player.entityRemoveQueue.toIntArray())))
                player.entityRemoveQueue.clear()
            }
        }
        flushEntityPackets(connection.player)
        worldManagers.values.forEach { flushEntityPackets(it.player) }

        // Flush view packets via main connection
        worldManagers.values.forEach { manager ->
            (manager.player as? ViewEntity)?.channel?.outboundMessages()?.onEach {
                WorldData(manager.world.provider.dimension, it as ByteBuf).sendTo(connection.player)
            }?.clear()
        }
    }

    override fun beginTransaction() {
        flushPackets()
        connection.sendPacket(SPacketCustomPayload("$MOD_ID|TS", PacketBuffer(Unpooled.EMPTY_BUFFER)))
        Transaction(Transaction.Phase.START).sendTo(player)
    }

    override fun endTransaction() {
        flushPackets()
        Transaction(Transaction.Phase.END).sendTo(player)
        connection.sendPacket(SPacketCustomPayload("$MOD_ID|TE", PacketBuffer(Unpooled.EMPTY_BUFFER)))
    }

    private inner class EventHandler {
        var registered by MinecraftForge.EVENT_BUS

        @SubscribeEvent
        fun onPlayerLeft(event: PlayerLoggedOutEvent) {
            if ((event.player as? EntityPlayerMP)?.connection === connection) {
                destroy()
            }
        }

        @SubscribeEvent
        fun postTick(event: TickEvent.ServerTickEvent) {
            if (event.phase != TickEvent.Phase.END) return

            tick()
        }

        @SubscribeEvent
        fun onWorldUnload(event: WorldEvent.Unload) {
            val manager = worldManagers[event.world] ?: return
            if (manager.player is ViewEntity) {
                destroyWorldManager(manager)
            }
        }

        @SubscribeEvent
        fun onPlayerRespawn(event: PlayerEvent.Clone) {
            val player = event.entityPlayer
            if (player is EntityPlayerMP && player.connection === connection) {
                val newWorld = player.serverWorld

                worldManagers.values.toList().forEach {
                    if (it.player is ViewEntity) {
                        destroyWorldManager(it)
                    } else {
                        worldManagers.remove(it.world)
                    }
                }

                worldManagers[newWorld] = ServerWorldManager(this@ServerWorldsManagerImpl, newWorld, player)
                this@ServerWorldsManagerImpl.player = player
            }
        }
    }
}

class VanillaView(
        override val manager: ServerWorldsManager,
        val player: EntityPlayerMP
) : View {
    override fun dispose(): Unit = throw UnsupportedOperationException("Cannot dispose of vanilla player view.")
    override val isValid: Boolean = true
    override val world: WorldServer = player.serverWorld
    override val center: Vec3d get() = player.pos
    override val cubeSelector: CubeSelector = player.server!!.playerList.let { playerList ->
        CuboidCubeSelector(
                center.toBlockPos().toCubePos(),
                playerList.viewDistance,
                if (world.isCubicWorld) playerList.verticalViewDistance else playerList.viewDistance
        )
    }
    override val anchor: Pair<WorldServer, Vec3i>? = null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as VanillaView

        if (cubeSelector != other.cubeSelector) return false

        return true
    }

    override fun hashCode(): Int = cubeSelector.hashCode()
}