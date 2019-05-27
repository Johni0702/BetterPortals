package de.johni0702.minecraft.betterportals.common.blocks

import de.johni0702.minecraft.betterportals.BetterPortalsMod
import de.johni0702.minecraft.betterportals.LOGGER
import de.johni0702.minecraft.betterportals.common.*
import net.minecraft.block.Block
import net.minecraft.entity.Entity
import net.minecraft.init.Blocks
import net.minecraft.util.EnumFacing
import net.minecraft.util.Rotation
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.ChunkPos
import net.minecraft.world.World
import net.minecraft.world.WorldServer
import net.minecraftforge.common.ForgeChunkManager
import java.util.concurrent.CompletableFuture
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Interface which can be inherited from by blocks which form arbitrarily shaped portal structures.
 */
interface PortalBlock<EntityType> where EntityType: Entity, EntityType: Portal.Linkable {
    /**
     * Type of block used as the portal block itself. Usually `this`.
     * Probably shouldn't be solid.
     */
    val portalBlock: Block

    /**
     * Type of block used for the portal frame.
     */
    val frameBlock: Block

    /**
     * Type of block used for the steps the player can walk onto after going through the portal.
     * Irrelevant for lying portals.
     */
    val frameStepsBlock: Block

    /**
     * The maximum distance between the start position and the border right above it which is being searched.
     * To ensure that the portal can be found from any start position inside it, this is also the maximum
     * (inner) height of any portal.
     * Additionally, this is also the maximum (inner) width of any portal.
     */
    val maxPortalSize: Int

    /**
     * The type of entity created by this block.
     */
    val entityType: Class<EntityType>

    /**
     * Returns the remote world for a portal created at [pos] in the [localWorld].
     * Does not create any portals or frames.
     * @param localWorld World of the local end of the portal
     * @param pos Position of one of the portal blocks of the local end of the portal
     * @return The remote world or `null` if the portal cannot be created for some reason
     */
    fun getRemoteWorldFor(localWorld: WorldServer, pos: BlockPos): WorldServer?

    /**
     * Create a new portal entity.
     */
    fun createPortalEntity(localEnd: Boolean, world: World, plane: EnumFacing.Plane, portalBlocks: Set<BlockPos>,
                           localDim: Int, localPos: BlockPos, localRot: Rotation): EntityType

    /**
     * Tries to create a new portal pair.
     *
     * @param localWorld World of the local end of the portal
     * @param pos Position of one of the portal blocks of the local end of the portal
     * @return `true` if the local portal has been created, `false` otherwise
     */
    fun tryToLinkPortals(localWorld: World, pos: BlockPos): Boolean {
        if (localWorld !is WorldServer) return false

        val (localBlocks, localAxis) = findPortalFrame(localWorld.makeBlockCache(), pos, false)
        if (localBlocks.isEmpty()) return false

        val localDim = localWorld.provider.dimension
        val localPos = localBlocks.minByAnyCoord()!!
        val localRot = localAxis.toFacing(EnumFacing.AxisDirection.POSITIVE).toRotation()
        val portalBlocks = localBlocks.mapTo(mutableSetOf()) { it.subtract(localPos).rotate(localRot.reverse) }

        val remoteWorld = getRemoteWorldFor(localWorld, pos) ?: return false
        val remoteDim = remoteWorld.provider.dimension
        val future = findOrCreateRemotePortal(localWorld, localPos, localAxis.perpendicularPlane, portalBlocks, remoteWorld)

        val localPortal = createPortalEntity(true, localWorld, localAxis.perpendicularPlane, portalBlocks, localDim, localPos, localRot)
        localPortal.localBlocks.forEach {
            localWorld.setBlockState(it, portalBlock.defaultState, 2)
        }
        localWorld.forceSpawnEntity(localPortal)

        future.handleAsync({ result, throwable ->
            if (throwable != null) {
                LOGGER.error("Finding remote portal frame: ", throwable)
                localPortal.setDead()
            }

            if (localPortal.isDead) return@handleAsync

            val (remotePos, remoteRot) = result
            val remotePortal = createPortalEntity(false, remoteWorld, localAxis.perpendicularPlane, portalBlocks, remoteDim, remotePos, remoteRot)
            remotePortal.localBlocks.forEach {
                remoteWorld.setBlockState(it, portalBlock.defaultState, 2)
            }
            remoteWorld.forceSpawnEntity(remotePortal)

            localPortal.link(remoteDim, remotePos, remoteRot)
            remotePortal.link(localDim, localPos, localRot)
        }, { localWorld.server.addScheduledTask(it) }).exceptionally { LOGGER.catching(it) }
        return true
    }

    /**
     * Searches for a fitting, unused portal frame in the [remoteWorld] or creates a new one if none is found.
     * The search is executed asynchronously to prevent blocking the server. Once the general search is completed,
     * a synchronous check is done to ensure that the found portal frames are still valid.
     *
     * @param localWorld The world in which the local end of the portal is located
     * @param localPos The position at which the local end of the portal is located
     * @param plane The plane in which the portal should be lying
     * @param portalBlocks Set of block positions relative to [localPos] and without any rotation of the local portal
     * @param remoteWorld The world in which the remote end of the portal is to be located or created
     * @return Future of Pair of the remote position and rotation of the newly found/created portal
     */
    fun findOrCreateRemotePortal(
            localWorld: WorldServer,
            localPos: BlockPos,
            plane: EnumFacing.Plane,
            portalBlocks: Set<BlockPos>,
            remoteWorld: WorldServer
    ): CompletableFuture<Pair<BlockPos, Rotation>> {
        // Calculate target position
        val movementFactor = localWorld.provider.movementFactor / remoteWorld.provider.movementFactor
        val maxY = remoteWorld.provider.actualHeight
        val remotePosition = BlockPos(
                (localPos.x * movementFactor).roundToInt()
                        .coerceIn(remoteWorld.worldBorder.minX().toInt() + 16, remoteWorld.worldBorder.maxX().toInt() - 16)
                        .coerceIn(-29999872, 29999872),
                localPos.y.coerceAtMost(maxY - (portalBlocks.map { it.y }.max() ?: 0)),
                (localPos.z * movementFactor).roundToInt()
                        .coerceIn(remoteWorld.worldBorder.minZ().toInt() + 16, remoteWorld.worldBorder.maxZ().toInt() - 16)
                        .coerceIn(-29999872, 29999872)
        )

        val searchDist = 64
        val remotePos0 = remotePosition.add(0, -remotePosition.y, 0)
        val remoteChunkPos = ChunkPos(remotePos0)

        // Create block cache and pre-load it with whole chunks
        val asyncBlockCache = remoteWorld.makeChunkwiseBlockCache()
        val maxDist = ceil((maxPortalSize + searchDist) / 16.0).toInt()
        val loadedChunks = (-maxDist..maxDist).flatMap { x ->
            (-maxDist..maxDist).map { z ->
                val chunkPos = remoteChunkPos.add(x, z)
                val completableFuture = CompletableFuture<ChunkPos>()
                remoteWorld.chunkProvider.loadChunk(chunkPos.x, chunkPos.z) {
                    asyncBlockCache[chunkPos.getBlock(0, 0, 0)]
                    completableFuture.complete(chunkPos)
                }
                completableFuture
            }
        }

        // Make sure the world isn't unloaded while we're searching (that would invalidate our remoteWorld reference)
        val ticket = ForgeChunkManager.requestTicket(BetterPortalsMod.INSTANCE, remoteWorld, ForgeChunkManager.Type.NORMAL)
        ForgeChunkManager.forceChunk(ticket, remoteChunkPos)

        return CompletableFuture.allOf(*loadedChunks.toTypedArray()).thenApplyAsync {
            // Find any existing frames (of right shape) within 129xmaxYx129
            val existingFrames = mutableListOf<Pair<BlockPos, Rotation>>()
            val checkedPositions = mutableSetOf<BlockPos>()

            // Also find any spots where the portal is placed nicely on the ground (in case we don't find any frames)
            val nicePositions = mutableListOf<Triple<BlockPos, Rotation, EnumFacing.Axis>>()

            val rotatedPortalBlocks: Map<Rotation, Set<BlockPos>> = Rotation.values().associateBy({ it }) { rot ->
                portalBlocks.mapTo(mutableSetOf()) { it.rotate(rot) }
            }

            BlockPos.MutableBlockPos.getAllInBoxMutable(
                    remotePos0.add(-searchDist, 0, -searchDist),
                    remotePos0.add(searchDist, maxY, searchDist)
            ).forEach { pos ->
                val block = asyncBlockCache[pos].block
                if (block == frameBlock) {
                    for (potentialStartDirection in plane.facings()) {
                        val portalPos = pos.offset(potentialStartDirection)
                        if (!checkedPositions.add(portalPos)) continue
                        for (rotation in Rotation.values()) {
                            val remoteBlocks = portalBlocks.mapTo(mutableSetOf()) {
                                it.rotate(rotation).add(portalPos)
                            }
                            for (axis in plane.perpendicularAxes) {
                                if (checkPortal(asyncBlockCache, remoteBlocks, axis, false)) {
                                    existingFrames.add(Pair(portalPos, rotation))
                                }
                            }
                        }
                    }
                } else if (block == Blocks.AIR) {
                    for ((rotation, blocks) in rotatedPortalBlocks) {
                        val axis = rotation.axis(plane.opposite)
                        if (considerPlacingPortalAt(asyncBlockCache, blocks, pos, axis)) {
                            nicePositions.add(Triple(pos.toImmutable(), rotation, axis))
                        }
                    }
                }
            }
            Pair(existingFrames, nicePositions)
        }.thenApplyAsync({ (existingFrames, nicePositions) ->
            val currentBlockCache = remoteWorld.makeBlockCache()
            // If any existing frames were found, use the nearest one (also check if it's still valid)
            existingFrames.sortedBy {
                it.first.distanceSq(remotePosition)
            }.firstOrNull { (portalPos, rotation) ->
                val remoteBlocks = portalBlocks.mapTo(mutableSetOf()) {
                    it.rotate(rotation).add(portalPos)
                }
                checkPortal(currentBlockCache, remoteBlocks, rotation.axis(plane.opposite), false)
            }?.let {
                return@thenApplyAsync Pair(it.first, it.second)
            }

            // Otherwise, place the portal at one of the positions which were determined to be good for that
            nicePositions.sortedBy {
                it.first.distanceSq(remotePosition)
            }.firstOrNull()?.let { (pos, rot, axis) ->
                val blocks = portalBlocks.mapTo(mutableSetOf()) {
                    it.rotate(rot).add(pos)
                }
                placePortalFrame(remoteWorld, axis, blocks)
                return@thenApplyAsync Pair(pos, rot)
            }

            // No better place found, place portal directly at target position with random rotation
            val portalRotation = Rotation.values()[Math.floorMod(localPos.x + localPos.z, 4)] // semi-random rotation
            val blocks = portalBlocks.mapTo(mutableSetOf()) {
                it.rotate(portalRotation).add(remotePosition)
            }
            placePortalFrame(remoteWorld, portalRotation.axis(plane.opposite), blocks)
            return@thenApplyAsync Pair(remotePosition, portalRotation)
        }, { remoteWorld.server.addScheduledTask(it) }).whenCompleteAsync({ _, _ ->
            // Finally, unforce the chunk we force to prevent the world from being unload
            ForgeChunkManager.releaseTicket(ticket)
            // and unload all chunks which we had to load just for finding the frame
            val chunkProvider = remoteWorld.chunkProvider
            val playerChunkMap = remoteWorld.playerChunkMap
            loadedChunks.forEach { futurePos ->
                val pos = futurePos.get()
                val chunk = chunkProvider.getLoadedChunk(pos.x, pos.z)
                if (playerChunkMap.getEntry(pos.x, pos.z) == null && chunk != null) {
                    chunkProvider.queueUnload(chunk)
                }
            }
        }, { remoteWorld.server.addScheduledTask(it) })
    }

    /**
     * Try to find an existing portal frame.
     * Note: This function is thread-safe if [blockCache] is.
     * @param blockCache Cache of blocks in which to check for the portal.
     * @param startPos Position at which the search is initiated from; needs to be one of the portal blocks (not frame!)
     * @param filled Whether to check if the portal is filled with [portalBlock] (or [Blocks.AIR])
     * @return Pair of a set of positions of all portal blocks (excluding frame and step blocks), empty if no frame was
     *         found, and the axis of the found portal frame
     */
    fun findPortalFrame(
            blockCache: BlockCache,
            startPos: BlockPos,
            filled: Boolean
    ): Pair<Set<BlockPos>, EnumFacing.Axis> {
        for (axis in EnumFacing.Axis.values()) {
            val result = findPortalFrame(blockCache, startPos, axis, filled)
            if (result.isNotEmpty()) {
                return Pair(result, axis)
            }
        }
        return Pair(emptySet(), EnumFacing.Axis.X)
    }

    /**
     * Try to find an existing portal frame.
     * Note: This function is thread-safe if [blockCache] is.
     * @param blockCache Cache of blocks in which to check for the portal.
     * @param startPos Position at which the search is initiated from; needs to be one of the portal blocks (not frame!)
     * @param axis Axis of the portal frame
     * @param filled Whether to check if the portal is filled with [portalBlock] (or [Blocks.AIR])
     * @return Set of positions of all portal blocks (excluding frame and step blocks), empty if no frame was found
     */
    fun findPortalFrame(
            blockCache: BlockCache,
            startPos: BlockPos,
            axis: EnumFacing.Axis,
            filled: Boolean
    ): Set<BlockPos> {
        val down = when(axis) {
            EnumFacing.Axis.X, EnumFacing.Axis.Z -> EnumFacing.DOWN
            EnumFacing.Axis.Y -> EnumFacing.SOUTH
        }
        val up = down.opposite

        val right = when(axis) {
            EnumFacing.Axis.X, EnumFacing.Axis.Z -> axis.toFacing(EnumFacing.AxisDirection.POSITIVE).rotateY()
            EnumFacing.Axis.Y -> EnumFacing.EAST
        }
        val left = right.opposite
        val directions = listOf(right, down, left, up)

        val filling = if (filled) portalBlock else Blocks.AIR

        fun BlockPos.maxDist(other: BlockPos): Int = max(abs(x - other.x), max(abs(y - other.y), abs(z - other.z)))

        val visitedBlocks = mutableSetOf<BlockPos>()
        val portalBlocks = mutableSetOf<BlockPos>()
        val blockQueue = mutableListOf(startPos)

        fun queueIfUnknown(pos: BlockPos) {
            if (pos !in visitedBlocks) {
                visitedBlocks.add(pos)
                blockQueue.add(pos)
            }
        }

        // Flood fill to find portal blocks and frame
        // Note that the flood is depth first in order to fail (relatively) fast
        loop@ while (blockQueue.isNotEmpty()) {
            val pos = blockQueue.takeLast()
            if (pos.maxDist(startPos) > maxPortalSize) return emptySet()
            when(blockCache[pos].block) {
                frameBlock -> continue@loop
                filling, Blocks.FIRE -> {
                    portalBlocks.add(pos)
                    directions.forEach {
                        queueIfUnknown(pos.offset(it))
                    }
                }
                else -> return emptySet()
            }
        }

        // Check that the portal is of valid size
        if (portalBlocks.toAxisAlignedBB().maxSideLength > maxPortalSize) return emptySet()

        // Done
        return portalBlocks
    }

    /**
     * Checks if the given portal may be placed at the given position.
     * The block at the given position will always be [Blocks.AIR].
     * Unless a perfectly fitting frame is found, the portal will be placed at a position which has been approved
     * by this method.
     * If no positions have been approved by this method, the portal will be placed exactly at the corresponding coords.
     * Note: This function must be thread-safe (i.e. only use [blockCache], not the world).
     * Also, no blocks further than [maxPortalSize] from [pos] may be accessed.
     * @param blockCache Cache of blocks in which to check for the portal
     * @param portalBlocks Set of positions of portal blocks (excluding frame and steps)
     * @param pos Position at which the portal is to be placed
     * @param axis The axis of the portal
     * @return `true` if the portal may be placed, `false` otherwise
     */
    fun considerPlacingPortalAt(
            blockCache: BlockCache,
            portalBlocks: Set<BlockPos>,
            pos: BlockPos,
            axis: EnumFacing.Axis
    ): Boolean {
        // Check if this is a block right above ground (or in case of horizontal portals, four above)
        // i.e. the lowest portal block is at `pos`
        if (!(if (axis == EnumFacing.Axis.Y) {
            (1..3).all { blockCache[pos.down(it)].block == Blocks.AIR } && blockCache[pos.down(4)].material.isSolid
        } else {
            blockCache[pos.down()].material.isSolid
        })) {
            return false
        }
        // Check if there's space for the portal and frame (except the bottom row for vertical portals)
        portalBlocks.map { it.add(pos) }.forEach { portalBlock ->
            if (blockCache[portalBlock].block != Blocks.AIR) return false
            axis.parallelFaces.forEach {
                val offsetPos = portalBlock.offset(it)
                if (offsetPos !in portalBlocks && offsetPos.y >= pos.y && blockCache[offsetPos].block != Blocks.AIR) {
                    return false
                }
            }
        }
        return true
    }

    /**
     * Checks if the given portal is still valid.
     * Note: This function is thread-safe if [blockCache] is.
     * @param blockCache Cache of blocks in which to check for the portal
     * @param portalBlocks Set of positions of portal blocks (excluding frame and steps)
     * @param axis The axis of the portal
     * @param filled Whether to check if the portal is filled with [portalBlock] (or [Blocks.AIR])
     * @return `true` if the portal is still valid, `false` otherwise
     */
    fun checkPortal(
            blockCache: BlockCache,
            portalBlocks: Set<BlockPos>,
            axis: EnumFacing.Axis,
            filled: Boolean
    ): Boolean {
        val filling = if (filled) portalBlock else Blocks.AIR
        portalBlocks.forEach { pos ->
            if (blockCache[pos].block != filling) return false
            axis.parallelFaces.forEach {
                val offsetPos = pos.offset(it)
                if (offsetPos !in portalBlocks && blockCache[offsetPos].block != frameBlock) {
                    return false
                }
            }
        }
        return true
    }

    /**
     * Places a new portal frame (include steps, excluding portal blocks) in the given world.
     * @param world The world in which the portal is to be placed
     * @param axis The axis of the portal
     * @param portalBlocks Set of positions of portal blocks (excluding frame and steps)
     */
    fun placePortalFrame(world: World, axis: EnumFacing.Axis, portalBlocks: Set<BlockPos>) {
        // Calculate frame blocks (thick/continuous frame)
        val frameBlocks = mutableSetOf<BlockPos>()
        portalBlocks.forEach { portalBlock ->
            // For each portal block look at all eight surrounding blocks in the plane defined by axis
            axis.parallelFaces.forEach { facing ->
                for (i in listOf(0, 1)) {
                    val pos = portalBlock.offset(facing).offset(facing.rotateAround(axis), i)
                    // And add all non-portal blocks as frame blocks
                    if (pos !in portalBlocks) {
                        frameBlocks.add(pos)
                    }
                }
            }
        }

        val minY = portalBlocks.map { it.y }.min() ?: 0

        // Make space inside, in front and behind of the portal and its frame
        (frameBlocks + portalBlocks).forEach { pos ->
            if (axis != EnumFacing.Axis.Y && pos.y < minY) {
                return@forEach // skip bottom-most layer for standing portals to keep blocks for the player to walk on
            }
            for (i in if (axis == EnumFacing.Axis.Y) -3..2 else -1..1) {
                world.setBlockToAir(pos.offset(axis.toFacing(EnumFacing.AxisDirection.POSITIVE), i))
            }
        }

        // Place frame blocks
        frameBlocks.forEach { pos ->
            world.setBlockState(pos, frameBlock.defaultState)
        }

        // Place step blocks for the player to step onto when leaving the portal
        if (axis != EnumFacing.Axis.Y) { // except for portals the player has to fall through
            frameBlocks.forEach { framePos ->
                // Steps are placed next to every frame block which has a portal block above it
                if (framePos.offset(EnumFacing.UP) in portalBlocks) {
                    for (direction in EnumFacing.AxisDirection.values()) {
                        val stepPos = framePos.offset(axis.toFacing(direction))
                        // Any other (already existing) solid blocks will do as well
                        if (!world.getBlockState(stepPos).material.isSolid) {
                            world.setBlockState(stepPos, frameStepsBlock.defaultState)
                        }
                    }
                }
            }
        }
    }

    /**
     * Checks if the portal, which the block at [pos] is part of, is still valid and if it is not, removes
     * the corresponding portal entity and block at [pos].
     * Note that the portal entity is expected to set all its portal blocks to air in/after [Entity.setDead].
     * Usually called from [Block.neighborChanged] to allow for recursive removal of the whole portal.
     * @param world The world in which the local portal is in
     * @param pos The position of one of the portal blocks which is to be checked.
     */
    fun validatePortalOrDestroy(world: World, pos: BlockPos) {
        if (world.isRemote) return
        val portalBlocks = findPortalFrame(world.makeBlockCache(), pos, true).first
        if (portalBlocks.isEmpty()) { // Portal shell broken, portal unrecognizable; flood fill with air
            world.getEntitiesWithinAABB(entityType, AxisAlignedBB(pos)).forEach {
                it.setDead()
            }
            world.setBlockToAir(pos)
        } else { // Portal shell still valid but shape or inners might have changed; needs to be check manually
            val entities = world.getEntitiesWithinAABB(entityType, portalBlocks.toAxisAlignedBB())
            if (entities.isEmpty()) { // No portals found, this is probably a vanilla portal which needs to be converted
                // First, empty the frame
                portalBlocks.forEach { world.setBlockToAir(it) }
                // Then try to link it to a remote portal as if the user has initiated that linkage
                tryToLinkPortals(world, pos)
            } else { // Check if the existing portal(s) are still valid
                entities.forEach {
                    if (it.isDead) return@forEach
                    if (!checkPortal(world.makeBlockCache(), it.localBlocks, it.localAxis, true)) {
                        it.setDead()
                    }
                }
            }
        }
    }
}