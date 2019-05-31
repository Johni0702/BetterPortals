package de.johni0702.minecraft.betterportals.server.view

import de.johni0702.minecraft.betterportals.LOGGER
import de.johni0702.minecraft.betterportals.common.AReferenceCounted
import de.johni0702.minecraft.betterportals.common.Utils.swapPosRot
import de.johni0702.minecraft.betterportals.net.ChangeServerMainView
import de.johni0702.minecraft.betterportals.net.Transaction
import de.johni0702.minecraft.betterportals.net.sendTo
import de.johni0702.minecraft.view.server.ServerView
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
        override val id: Int,
        override var camera: EntityPlayerMP,
        var channel: EmbeddedChannel?
) : ServerView, AReferenceCounted {
    override var refCnt: Int = 1

    override fun doRelease() {
        // ServerViewImpls with refCnt 0 are cleaned up once per tick only.
        // That way, the view can be reused by anyone else for one tick even if it's been released already
        if (isMainView) {
            LOGGER.warn("Main view of $camera has been released at: ", Throwable())
        }
    }

    override fun makeMainView() {
        if (isMainView) return

        val mainView = manager.mainView
        val player = mainView.camera
        val camera = this.camera

        LOGGER.info("Swapping main view {}/{}/{} with {}/{}/{}",
                player.posX, player.posY, player.posZ,
                camera.posX, camera.posY, camera.posZ)

        retain()

        val oldDim = player.dimension
        val oldWorld = player.serverWorld
        val newDim = camera.dimension
        val newWorld = camera.serverWorld

        manager.flushPackets()
        Transaction.start(player)

        // TODO set enteredNetherPosition (see EntityPlayerMP#changeDimension)

        fun unregister(player: EntityPlayerMP): Pair<List<PlayerChunkMapEntry>, List<EntityTrackerEntry>> {
            val posX = player.managedPosX.toInt() shr 4
            val posZ = player.managedPosZ.toInt() shr 4
            val viewRadius = manager.server.playerList.viewDistance
            val playerChunkMap = player.serverWorld.playerChunkMap
            val knownChunks = mutableListOf<PlayerChunkMapEntry>()
            for (x in posX - viewRadius..posX + viewRadius) {
                for (z in posZ - viewRadius..posZ + viewRadius) {
                    val entry = playerChunkMap.getEntry(x, z)
                    if (entry != null && entry.players.remove(player)) {
                        if (entry.isSentToPlayers) {
                            MinecraftForge.EVENT_BUS.post(ChunkWatchEvent.UnWatch(entry.chunk, player))
                        }
                        knownChunks.add(entry)
                    }
                }
            }

            playerChunkMap.players.remove(player)

            val knownEntities = mutableListOf<EntityTrackerEntry>()
            player.serverWorld.entityTracker.entries.forEach { entry ->
                if (entry.trackingPlayers.remove(player)) {
                    entry.trackedEntity.removeTrackingPlayer(player)
                    knownEntities.add(entry)
                }
            }

            return Pair(knownChunks, knownEntities)
        }
        // Important: Unregister the camera before the player because the camera depends on the player
        val newRegistrations = unregister(camera)
        val oldRegistrations = unregister(player)

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

        swapPosRot(player, camera)

        player.managedPosX = camera.managedPosX.also { camera.managedPosX = player.managedPosX }
        player.managedPosZ = camera.managedPosZ.also { camera.managedPosZ = player.managedPosZ }

        player.connection.captureCurrentPosition()

        player.dimension = newDim
        camera.dimension = oldDim

        player.setWorld(newWorld)
        camera.setWorld(oldWorld)
        player.interactionManager.setWorld(newWorld)
        camera.interactionManager.setWorld(oldWorld)

        fun register(player: EntityPlayerMP, knownRegistrations: Pair<List<PlayerChunkMapEntry>, List<EntityTrackerEntry>>) {
            val (knownChunks, knownEntities) = knownRegistrations

            knownChunks.forEach {
                it.players.add(player)
                if (it.isSentToPlayers) {
                    MinecraftForge.EVENT_BUS.post(ChunkWatchEvent.Watch(it.chunk, player))
                }
            }
            player.serverWorld.playerChunkMap.players.add(player)

            knownEntities.forEach {
                it.trackingPlayers.add(player)
                it.trackedEntity.addTrackingPlayer(player)
            }
        }
        // Important: Add the player before the camera because the camera depends on the player
        register(player, newRegistrations)
        register(camera, oldRegistrations)

        newWorld.spawnEntity(player)
        oldWorld.spawnEntity(camera)

        CriteriaTriggers.CHANGED_DIMENSION.trigger(player, oldWorld.provider.dimensionType, newWorld.provider.dimensionType)
        if (oldWorld.provider.dimensionType == DimensionType.NETHER
                && newWorld.provider.dimensionType == DimensionType.OVERWORLD) {
            CriteriaTriggers.NETHER_TRAVEL.trigger(player, Vec3d(player.posX, player.posY, player.posZ))
        }

        Transaction.end(player)

        manager.flushPackets() // Just for good measure, who knows what other mods will do during the event
        FMLCommonHandler.instance().firePlayerChangedDimensionEvent(player, oldDim, newDim)

        mainView.release()
    }
}