package de.johni0702.minecraft.betterportals.impl.tf.common.entity

import de.johni0702.minecraft.betterportals.common.entity.OneWayPortalEntity
import net.minecraft.block.Block
import net.minecraft.init.Blocks
import net.minecraft.util.EnumFacing
import net.minecraft.util.Rotation
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import twilightforest.block.TFBlocks

class TFPortalEntity(
        isTailEnd: Boolean, world: World, relativeBlocks: Set<BlockPos>,
        localDimension: Int, localPosition: BlockPos, localRotation: Rotation,
        remoteDimension: Int?, remotePosition: BlockPos, remoteRotation: Rotation
) : OneWayPortalEntity(
        isTailEnd, world, EnumFacing.Plane.HORIZONTAL, relativeBlocks,
        localDimension, localPosition, localRotation,
        remoteDimension, remotePosition, remoteRotation
) {
    @Suppress("unused")
    constructor(world: World) : this(false, world, emptySet(), 0, BlockPos.ORIGIN, Rotation.NONE, null, BlockPos.ORIGIN, Rotation.NONE)

    override val portalFrameBlock: Block get() = Blocks.GRASS

    override fun onUpdate() {
        super.onUpdate()
        // Prevent legacy portal blocks from rendering on the client when we have a better portal
        if (world.isRemote && !isDead) {
            // Cannot replace with AIR because we still need the block light
            val replacementState = Blocks.PORTAL.defaultState
            localBlocks.forEach {
                if (world.getBlockState(it).block == TFBlocks.portal) {
                    world.setBlockState(it, replacementState)
                }
            }
        }
    }

    override fun removePortal() {
        if (!isTailEnd) {
            localBlocks.forEach { world.setBlockState(it, Blocks.WATER.defaultState) }
        }
    }
}