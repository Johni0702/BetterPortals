package de.johni0702.minecraft.view.impl.server

import de.johni0702.minecraft.betterportals.common.forceAddEntity
import de.johni0702.minecraft.betterportals.common.forceRemoveEntity
import de.johni0702.minecraft.betterportals.common.haveCubicChunks
import de.johni0702.minecraft.betterportals.impl.accessors.AccNetHandlerPlayServer
import de.johni0702.minecraft.view.impl.LOGGER
import de.johni0702.minecraft.view.impl.net.ChangeServerMainWorld
import de.johni0702.minecraft.view.impl.net.sendTo
import de.johni0702.minecraft.view.impl.worldsManagerImpl
import de.johni0702.minecraft.view.server.CubeSelector
import de.johni0702.minecraft.view.server.View
import io.github.opencubicchunks.cubicchunks.core.server.CubeWatcher
import net.minecraft.advancements.CriteriaTriggers
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.util.math.ChunkPos
import net.minecraft.util.math.Vec3d
import net.minecraft.util.math.Vec3i
import net.minecraft.world.DimensionType
import net.minecraft.world.WorldServer
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.world.ChunkWatchEvent

//#if MC>=11400
//$$ import net.minecraftforge.fml.hooks.BasicEventHooks
//$$ import de.johni0702.minecraft.view.impl.mixin.AccessorChunkManager
//$$ import de.johni0702.minecraft.view.impl.mixin.AccessorEntityTracker
//$$ import de.johni0702.minecraft.view.impl.mixin.AccessorServerChunkProvider
//$$ import de.johni0702.minecraft.view.server.CuboidCubeSelector
//#else
import de.johni0702.minecraft.betterportals.impl.accessors.AccEntityTracker
import de.johni0702.minecraft.betterportals.impl.accessors.AccPlayerChunkMap
import de.johni0702.minecraft.betterportals.impl.accessors.AccPlayerChunkMapEntry
import de.johni0702.minecraft.view.impl.mixin.AccessorCubeWatcher_CC
import io.github.opencubicchunks.cubicchunks.core.server.PlayerCubeMap
import net.minecraft.entity.EntityTrackerEntry
import net.minecraft.server.management.PlayerChunkMapEntry
import net.minecraftforge.fml.common.FMLCommonHandler
//#endif

internal class ServerWorldManager(
        private val manager: ServerWorldsManagerImpl,
        val world: WorldServer,
        var player: EntityPlayerMP
) {
    val views = mutableListOf<View>()
    var activeViews = mutableMapOf<View, Int>()
    var activeSelectors = mutableSetOf<CubeSelector>()
        set(value) {
            if (field == value) return

            // Selectors changed, need to run a full update for that world
            // TODO given at this point we know the old selectors, we could probably do some diffing for improved
            //  performance if needed, considering this happens whenever someone walks 16 blocks, it may or may not
            //  be worth it
            needsUpdate = true

            field = value
        }

    //#if MC>=11400
    //$$ interface ITicketManager {
    //$$     fun addCuboidView(player: ServerPlayerEntity, selector: CuboidCubeSelector)
    //$$     fun removeCuboidView(player: ServerPlayerEntity, selector: CuboidCubeSelector)
    //$$     fun getSourceLevelForChunk(chunkPos: Long): Int
    //$$ }
    //$$ var trackedSelectors = mutableSetOf<CuboidCubeSelector>()
    //#endif

    var trackedColumns = mutableSetOf<ChunkPos>()
    var trackedCubes = mutableSetOf<Vec3i>()

    var needsUpdate = false

    //#if MC>=11400
    //$$ interface ColumnTrackingUpdater { operator fun invoke(chunkPos: ChunkPos, load: Boolean) }
    //$$ fun updateTrackedColumns(updater: ColumnTrackingUpdater) {
    //$$     val ticketManager = ((world.chunkProvider as AccessorServerChunkProvider).ticketManager) as ITicketManager
    //$$     val updatedSelectors = mutableSetOf<CuboidCubeSelector>()
    //$$     for (selector in trackedSelectors) {
    //$$         if (selector in activeSelectors) {
    //$$             updatedSelectors.add(selector)
    //$$         } else {
    //$$             ticketManager.removeCuboidView(player, selector)
    //$$         }
    //$$     }
    //$$     for (selector in activeSelectors) {
    //$$         if (updatedSelectors.add(selector as? CuboidCubeSelector ?: continue)) {
    //$$             ticketManager.addCuboidView(player, selector)
    //$$         }
    //$$     }
    //$$
    //$$     updateTrackedColumns({ updater(it, true) }, { updater(it, false) })
    //$$ }
    //#else
    fun updateTrackedColumns(getOrCreateEntry: (ChunkPos) -> PlayerChunkMapEntry) {
        updateTrackedColumns({ getOrCreateEntry(it).addPlayer(player) }, { getOrCreateEntry(it).removePlayer(player) })
    }
    //#endif

    private fun updateTrackedColumns(track: (ChunkPos) -> Unit, untrack: (ChunkPos) -> Unit) {
        val updatedColumns = mutableSetOf<ChunkPos>()

        // Always need to keep our current chunk loaded (even for views) as otherwise we'll be removed from the world
        val selfPos = with(player) { ChunkPos(chunkCoordX, chunkCoordZ) }
        updatedColumns.add(selfPos)
        if (selfPos !in trackedColumns) {
            track(selfPos)
        }

        activeSelectors.forEach {
            it.forEachColumn { pos ->
                if (updatedColumns.add(pos)) {
                    if (pos !in trackedColumns) {
                        track(pos)
                    }
                }
            }
        }

        trackedColumns.forEach { pos ->
            if (pos !in updatedColumns) {
                untrack(pos)
            }
        }

        trackedColumns = updatedColumns
    }

    //#if MC>=11400
    //$$ // TODO 1.14 CC
    //#else
    fun updateTrackedColumnsAndCubes(
            getOrCreateColumnWatcher: (ChunkPos) -> PlayerChunkMapEntry,
            getOrCreateCubeWatcher: (Vec3i) -> CubeWatcher
    ) {
        val getOrCreateCubeWatcherAccessor = { pos: Vec3i ->
            getOrCreateCubeWatcher(pos) as AccessorCubeWatcher_CC
        }
        val updatedColumns = mutableSetOf<ChunkPos>()
        val updatedCubes = mutableSetOf<Vec3i>()

        // Always need to keep our current cube loaded (even for views) as otherwise we'll be removed from the world
        val selfColumnPos = with(player) { ChunkPos(chunkCoordX, chunkCoordZ) }
        val selfCubePos = with(player) { Vec3i(chunkCoordX, chunkCoordY, chunkCoordZ) }
        updatedColumns.add(selfColumnPos)
        updatedCubes.add(selfCubePos)
        if (selfColumnPos !in trackedColumns) {
            getOrCreateColumnWatcher(selfColumnPos).addPlayer(player)
        }
        if (selfCubePos !in trackedCubes) {
            getOrCreateCubeWatcherAccessor(selfCubePos).invokeAddPlayer(player)
        }

        activeSelectors.forEach {
            it.forEachCube { cubePos ->
                if (updatedCubes.add(cubePos)) {
                    val columnPos = ChunkPos(cubePos.x, cubePos.z)
                    if (updatedColumns.add(columnPos)) {
                        if (columnPos !in trackedColumns) {
                            getOrCreateColumnWatcher(columnPos).addPlayer(player)
                        }
                    }
                    if (cubePos !in trackedCubes) {
                        getOrCreateCubeWatcherAccessor(cubePos).invokeAddPlayer(player)
                    }
                }
            }
        }

        trackedCubes.forEach { cubePos ->
            if (cubePos !in updatedCubes) {
                getOrCreateCubeWatcherAccessor(cubePos).invokeRemovePlayer(player)
            }
        }

        trackedColumns.forEach { columnPos ->
            if (columnPos !in updatedColumns) {
                getOrCreateColumnWatcher(columnPos).removePlayer(player)
            }
        }

        trackedColumns = updatedColumns
        trackedCubes = updatedCubes
    }
    //#endif

    internal fun makeMainWorld(updatePosition: EntityPlayerMP.() -> Unit) {
        check(player is ViewEntity) { "makeMainWorld called on main view" }

        val main = manager.mainWorldManager
        val player = main.player
        val camera = this.player

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

        oldWorld.forceRemoveEntity(player)
        newWorld.forceRemoveEntity(camera)

        player.isDead = false
        camera.isDead = false

        // Make sure all packets which should have been sent by now are actually sent for the correct view
        manager.flushPackets()

        this.player = main.player.also { main.player = this.player }

        ChangeServerMainWorld(world.provider.dimension).sendTo(player)

        manager.server.playerList.updatePermissionLevel(player)

        player.dimension = newDim
        camera.dimension = oldDim

        updatePosition(player)

        (player.connection as AccNetHandlerPlayServer).invokeCaptureCurrentPosition()

        player.setWorld(newWorld)
        camera.setWorld(oldWorld)
        player.interactionManager.setWorld(newWorld)
        camera.interactionManager.setWorld(oldWorld)

        // Important: Add the player before the camera because the camera depends on the player
        newRegistrations.forEach { it(player) }
        oldRegistrations.forEach { it(camera) }

        newWorld.forceAddEntity(player)
        oldWorld.forceAddEntity(camera)

        //#if MC>=11400
        //$$ val fromType = oldWorld.dimension.type
        //$$ val toType = newWorld.dimension.type
        //#else
        val fromType = oldWorld.provider.dimensionType
        val toType = newWorld.provider.dimensionType
        //#endif
        CriteriaTriggers.CHANGED_DIMENSION.trigger(player, fromType, toType)
        if (fromType == DimensionType.NETHER && toType == DimensionType.OVERWORLD) {
            CriteriaTriggers.NETHER_TRAVEL.trigger(player, Vec3d(player.posX, player.posY, player.posZ))
        }

        manager.endTransaction()

        manager.flushPackets() // Just for good measure, who knows what other mods will do during the event
        //#if MC>=11400
        //$$ BasicEventHooks.firePlayerChangedDimensionEvent(player, oldDim, newDim)
        //#else
        FMLCommonHandler.instance().firePlayerChangedDimensionEvent(player, oldDim, newDim)
        //#endif
    }
}

internal interface SwapHandler {
    fun swap(prevPlayer: EntityPlayerMP): (newPlayer: EntityPlayerMP) -> Unit
}

internal object EntityTrackerHandler : SwapHandler {
    override fun swap(prevPlayer: EntityPlayerMP): (EntityPlayerMP) -> Unit {
        //#if MC>=11400
        //$$ val knownEntities = mutableListOf<AccessorEntityTracker>()
        //$$ val entries = (prevPlayer.serverWorld.chunkProvider.chunkManager as AccessorChunkManager).entities.values
        //#else
        val knownEntities = mutableListOf<EntityTrackerEntry>()
        val entries = (prevPlayer.serverWorld.entityTracker as AccEntityTracker).entries
        //#endif
        entries.forEach { entry ->
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
    //#if MC>=11400
    //$$ interface IChunkManager {
    //$$     fun removePlayerForSwap(player: ServerPlayerEntity)
    //$$     fun addPlayerForSwap(player: ServerPlayerEntity)
    //$$ }
    //#endif

    override fun swap(prevPlayer: EntityPlayerMP): (EntityPlayerMP) -> Unit {
        //#if MC>=11400
        //$$ (prevPlayer.serverWorld.chunkProvider.chunkManager as IChunkManager).removePlayerForSwap(prevPlayer)
        //$$ return { newPlayer ->
        //$$     (newPlayer.serverWorld.chunkProvider.chunkManager as IChunkManager).addPlayerForSwap(newPlayer)
        //$$ }
        //#else
        val world = prevPlayer.serverWorld
        val worldManager = prevPlayer.worldsManagerImpl.worldManagers.getValue(world)
        val playerChunkMap = world.playerChunkMap
        val knownChunks = mutableListOf<PlayerChunkMapEntry>()
        worldManager.trackedColumns.forEach {
            val entry = playerChunkMap.getEntry(it.x, it.z)
            if (entry != null && (entry as AccPlayerChunkMapEntry).players.remove(prevPlayer)) {
                if (entry.isSentToPlayers) {
                    MinecraftForge.EVENT_BUS.post(ChunkWatchEvent.UnWatch(entry.chunk, prevPlayer))
                }
                knownChunks.add(entry)
            }
        }

        (playerChunkMap as AccPlayerChunkMap).players.remove(prevPlayer)

        return { newPlayer ->
            knownChunks.forEach {
                (it as AccPlayerChunkMapEntry).players.add(newPlayer)
                if (it.isSentToPlayers) {
                    MinecraftForge.EVENT_BUS.post(ChunkWatchEvent.Watch(it.chunk, newPlayer))
                }
            }
            (newPlayer.serverWorld.playerChunkMap as AccPlayerChunkMap).players.add(newPlayer)
        }
        //#endif
    }
}

internal object PlayerCubeMapHandler : SwapHandler {
    /**
     * This is where the magic happens. When this is `true`, we suppress the ordinary chunk (un-)load packets because
     * we don't need them (the client will just swap its world and then it'll already have them).
     */
    var swapInProgress = false

    override fun swap(prevPlayer: EntityPlayerMP): (newPlayer: EntityPlayerMP) -> Unit {
        //#if MC>=11400
        //$$ TODO("1.14 CC")
        //#else
        val playerChunkMap = prevPlayer.serverWorld.playerChunkMap
                as? PlayerCubeMap
                ?: return PlayerChunkMapHandler.swap(prevPlayer)

        swapInProgress = true
        playerChunkMap.removePlayer(prevPlayer)
        swapInProgress = false

        return { newPlayer ->
            swapInProgress = true
            playerChunkMap.addPlayer(newPlayer)
            swapInProgress = false
        }
        //#endif
    }
}

//#if MC<11400
// This is here as a workaround for https://github.com/SpongePowered/Mixin/issues/305 or (or 288, not sure)
fun CubeWatcher.removePlayer(player: EntityPlayerMP) {
    (this as AccessorCubeWatcher_CC).invokeRemovePlayer(player)
}
fun CubeWatcher.addPlayer(player: EntityPlayerMP) {
    (this as AccessorCubeWatcher_CC).invokeAddPlayer(player)
}
//#endif