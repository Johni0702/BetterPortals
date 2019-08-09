package de.johni0702.minecraft.view.impl.server

import de.johni0702.minecraft.betterportals.common.haveCubicChunks
import de.johni0702.minecraft.view.impl.LOGGER
import de.johni0702.minecraft.view.impl.common.swapPosRotWith
import de.johni0702.minecraft.view.impl.net.ChangeServerMainView
import de.johni0702.minecraft.view.impl.net.sendTo
import de.johni0702.minecraft.view.server.*
import io.github.opencubicchunks.cubicchunks.core.server.PlayerCubeMap
import io.netty.channel.embedded.EmbeddedChannel
import net.minecraft.advancements.CriteriaTriggers
import net.minecraft.entity.EntityTrackerEntry
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.server.management.PlayerChunkMapEntry
import net.minecraft.util.math.Vec3d
import net.minecraft.world.DimensionType
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.world.ChunkWatchEvent
import net.minecraftforge.fml.common.FMLCommonHandler

internal class ServerViewImpl(
        override val manager: ServerViewManagerImpl,
        id: Int,
        override var camera: EntityPlayerMP,
        var channel: EmbeddedChannel?
) : ServerView {
    override var isValid = true
    override val id: Int = id
        get() = checkValid().let { field }
    internal var tickets = mutableListOf<TicketImpl>()
    private var fixedLocationTickets = 0
    private var exclusiveTickets = 0

    override fun allocatePlainTicket(): Ticket = checkValid().let { TicketImpl(this).also { tickets.add(it) } }

    override fun allocateFixedLocationTicket(): FixedLocationTicket? = checkValid().let {
        if (exclusiveTickets > 0) {
            null
        } else {
            fixedLocationTickets += 1
            FixedLocationTicketImpl(this).also { tickets.add(it) }
        }
    }

    override fun allocateExclusiveTicket(): ExclusiveTicket? = checkValid().let {
        if (fixedLocationTickets > 0 || exclusiveTickets > 0) {
            null
        } else {
            exclusiveTickets += 1
            ExclusiveTicketImpl(this).also { tickets.add(it) }
        }
    }

    internal fun releaseTicket(ticket: TicketImpl) {
        tickets.remove(ticket)
        if (ticket is FixedLocationTicket) {
            fixedLocationTickets -= 1
        }
        if (ticket is ExclusiveTicket) {
            exclusiveTickets -= 1
        }
    }

    override fun makeMainView(ticket: CanMakeMainView) {
        checkValid()
        ticket.ensureValid(this)
        makeMainView()
    }

    override fun releaseAndMakeMainView(ticket: CanMakeMainView) {
        checkValid()
        ticket.ensureValid(this)
        ticket.release()
        makeMainView()
    }

    private fun makeMainView() {
        if (isMainView) {
            throw IllegalStateException("makeMainView called on main view")
        }

        val mainView = manager.mainView
        val player = mainView.camera
        val camera = this.camera

        LOGGER.info("Swapping main view {}/{}/{} with {}/{}/{}",
                player.posX, player.posY, player.posZ,
                camera.posX, camera.posY, camera.posZ)

        val oldDim = player.dimension
        val oldWorld = player.serverWorld
        val newDim = camera.dimension
        val newWorld = camera.serverWorld

        manager.beginTransaction()

        // TODO set enteredNetherPosition (see EntityPlayerMP#changeDimension)

        val chunkMapHandler = if (haveCubicChunks) PlayerCubeMapHandler else PlayerChunkMapHandler
        val swapHandlers = listOf(chunkMapHandler, EntityTrackerHandler)

        // Important: Unregister the camera before the player because the camera depends on the player
        val newRegistrations = swapHandlers.map { it.swap(camera) }
        val oldRegistrations = swapHandlers.map { it.swap(player) }

        oldWorld.removeEntityDangerously(player)
        newWorld.removeEntityDangerously(camera)

        player.isDead = false
        camera.isDead = false

        // Make sure all packets which should have been sent by now are actually sent for the correct view
        manager.flushPackets()

        this.camera = mainView.camera.also { mainView.camera = this.camera }
        this.channel = mainView.channel.also { mainView.channel = this.channel }

        ChangeServerMainView(id).sendTo(player)

        manager.mainView = this

        manager.server.playerList.updatePermissionLevel(player)

        player.swapPosRotWith(camera)

        player.connection.captureCurrentPosition()

        player.dimension = newDim
        camera.dimension = oldDim

        player.setWorld(newWorld)
        camera.setWorld(oldWorld)
        player.interactionManager.setWorld(newWorld)
        camera.interactionManager.setWorld(oldWorld)

        // Important: Add the player before the camera because the camera depends on the player
        newRegistrations.forEach { it(player) }
        oldRegistrations.forEach { it(camera) }

        newWorld.spawnEntity(player)
        oldWorld.spawnEntity(camera)

        CriteriaTriggers.CHANGED_DIMENSION.trigger(player, oldWorld.provider.dimensionType, newWorld.provider.dimensionType)
        if (oldWorld.provider.dimensionType == DimensionType.NETHER
                && newWorld.provider.dimensionType == DimensionType.OVERWORLD) {
            CriteriaTriggers.NETHER_TRAVEL.trigger(player, Vec3d(player.posX, player.posY, player.posZ))
        }

        manager.endTransaction()

        manager.flushPackets() // Just for good measure, who knows what other mods will do during the event
        FMLCommonHandler.instance().firePlayerChangedDimensionEvent(player, oldDim, newDim)
    }
}

internal interface SwapHandler {
    fun swap(prevPlayer: EntityPlayerMP): (newPlayer: EntityPlayerMP) -> Unit
}

internal object EntityTrackerHandler : SwapHandler {
    override fun swap(prevPlayer: EntityPlayerMP): (EntityPlayerMP) -> Unit {
        val knownEntities = mutableListOf<EntityTrackerEntry>()
        prevPlayer.serverWorld.entityTracker.entries.forEach { entry ->
            if (entry.trackingPlayers.remove(prevPlayer)) {
                entry.trackedEntity.removeTrackingPlayer(prevPlayer)
                knownEntities.add(entry)
            }
        }
        return { newPlayer ->
            knownEntities.forEach {
                it.trackingPlayers.add(newPlayer)
                it.trackedEntity.addTrackingPlayer(newPlayer)
            }
        }
    }
}

internal object PlayerChunkMapHandler : SwapHandler {
    override fun swap(prevPlayer: EntityPlayerMP): (EntityPlayerMP) -> Unit {
        val managedPosX = prevPlayer.managedPosX
        val managedPosZ = prevPlayer.managedPosZ
        val chunkPosX = managedPosX.toInt() shr 4
        val chunkPosZ = managedPosZ.toInt() shr 4
        val viewRadius = prevPlayer.mcServer.playerList.viewDistance
        val playerChunkMap = prevPlayer.serverWorld.playerChunkMap
        val knownChunks = mutableListOf<PlayerChunkMapEntry>()
        for (x in chunkPosX - viewRadius..chunkPosX + viewRadius) {
            for (z in chunkPosZ - viewRadius..chunkPosZ + viewRadius) {
                val entry = playerChunkMap.getEntry(x, z)
                if (entry != null && entry.players.remove(prevPlayer)) {
                    if (entry.isSentToPlayers) {
                        MinecraftForge.EVENT_BUS.post(ChunkWatchEvent.UnWatch(entry.chunk, prevPlayer))
                    }
                    knownChunks.add(entry)
                }
            }
        }

        playerChunkMap.players.remove(prevPlayer)

        return { newPlayer ->
            newPlayer.managedPosX = managedPosX
            newPlayer.managedPosZ = managedPosZ
            knownChunks.forEach {
                it.players.add(newPlayer)
                if (it.isSentToPlayers) {
                    MinecraftForge.EVENT_BUS.post(ChunkWatchEvent.Watch(it.chunk, newPlayer))
                }
            }
            newPlayer.serverWorld.playerChunkMap.players.add(newPlayer)
        }
    }
}

internal object PlayerCubeMapHandler : SwapHandler {
    /**
     * Ordinarily CC would run its chunk GC when we call [PlayerCubeMap.updateMovingPlayer]. Since we're calling
     * from a non-standard location, that might not be safe though.
     */
    var suppressChunkGc = false

    /**
     * This is where the magic happens. When this is `true`, we suppress the ordinary chunk (un-)load packets because
     * we don't need them (the client will just swap its world and then it'll already have them).
     */
    var swapInProgress = false

    override fun swap(prevPlayer: EntityPlayerMP): (newPlayer: EntityPlayerMP) -> Unit {
        val playerChunkMap = prevPlayer.serverWorld.playerChunkMap
                as? PlayerCubeMap
                ?: return PlayerChunkMapHandler.swap(prevPlayer)


        // Make sure their managedPos matches their pos
        // Irrelevant for vanilla as we'll swap the exact managedPos anyway but crucial for CubicChunks because
        // access to its internals is quite difficult (i.e. would require lots of reflection).
        suppressChunkGc = true
        playerChunkMap.updateMovingPlayer(prevPlayer)
        suppressChunkGc = false

        swapInProgress = true
        playerChunkMap.removePlayer(prevPlayer)
        swapInProgress = false

        return { newPlayer ->
            swapInProgress = true
            playerChunkMap.addPlayer(newPlayer)
            swapInProgress = false
        }
    }
}