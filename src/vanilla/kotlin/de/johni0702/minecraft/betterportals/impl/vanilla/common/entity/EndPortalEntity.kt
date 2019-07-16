package de.johni0702.minecraft.betterportals.impl.vanilla.common.entity

import de.johni0702.minecraft.betterportals.common.entity.OneWayPortalEntity
import de.johni0702.minecraft.betterportals.impl.vanilla.common.END_PORTAL_CONFIG
import net.minecraft.block.Block
import net.minecraft.init.Blocks
import net.minecraft.util.EnumFacing
import net.minecraft.util.Rotation
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import kotlin.math.abs

abstract class EndPortalEntity(
        isTailEnd: Boolean, world: World, relativeBlocks: Set<BlockPos>,
        localDimension: Int, localPosition: BlockPos, localRotation: Rotation,
        remoteDimension: Int, remotePosition: BlockPos, remoteRotation: Rotation
) : OneWayPortalEntity(
        isTailEnd, world, EnumFacing.Plane.HORIZONTAL, relativeBlocks,
        localDimension, localPosition, localRotation,
        remoteDimension,remotePosition, remoteRotation,
        END_PORTAL_CONFIG
)

class EndEntryPortalEntity(
        world: World,
        localDimension: Int, localPosition: BlockPos, localRotation: Rotation,
        remoteDimension: Int, remotePosition: BlockPos, remoteRotation: Rotation
) : EndPortalEntity(
        localDimension == 1, world,
        (0 until 3).flatMap { x -> (0 until 3).map { z -> BlockPos(-x, 0, -z) } }.toSet(),
        localDimension, localPosition, localRotation,
        remoteDimension, remotePosition, remoteRotation
) {
    @Suppress("unused")
    constructor(world: World) : this(world, 0, BlockPos.ORIGIN, Rotation.NONE, 0, BlockPos.ORIGIN, Rotation.NONE)

    override val portalFrameBlock: Block
        get() = Blocks.END_PORTAL_FRAME
}

class EndExitPortalEntity(
        world: World,
        localDimension: Int, localPosition: BlockPos, localRotation: Rotation,
        remoteDimension: Int, remotePosition: BlockPos, remoteRotation: Rotation
) : EndPortalEntity(
        remoteDimension == 1, world,
        (-2..2).flatMap { x -> (-2..2).mapNotNull { z ->
            if (abs(x) == 2 && abs(z) == 2 || x == 0 && z == 0) null
            else BlockPos(-x-2, 0, -z-2)
        } }.toSet(),
        localDimension, localPosition, localRotation,
        remoteDimension, remotePosition, remoteRotation
) {
    @Suppress("unused")
    constructor(world: World) : this(world, 0, BlockPos.ORIGIN, Rotation.NONE, 0, BlockPos.ORIGIN, Rotation.NONE)

    override val portalFrameBlock: Block
        get() = Blocks.BEDROCK
}
