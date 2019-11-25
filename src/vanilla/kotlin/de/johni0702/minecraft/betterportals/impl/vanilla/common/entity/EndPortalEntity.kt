package de.johni0702.minecraft.betterportals.impl.vanilla.common.entity

import de.johni0702.minecraft.betterportals.common.FinitePortal
import de.johni0702.minecraft.betterportals.common.entity.OneWayPortalEntity
import de.johni0702.minecraft.betterportals.common.util.TickTimer
import de.johni0702.minecraft.betterportals.impl.vanilla.common.END_PORTAL_CONFIG
import net.minecraft.block.Block
import net.minecraft.block.BlockEndPortal
import net.minecraft.init.Blocks
import net.minecraft.util.EnumFacing
import net.minecraft.util.Rotation
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import kotlin.math.abs

abstract class EndPortalEntity(
        isTailEnd: Boolean, world: World, relativeBlocks: Set<BlockPos>,
        localDimension: Int, localPosition: BlockPos, localRotation: Rotation
) : OneWayPortalEntity(
        isTailEnd, world,
        FinitePortal(EnumFacing.Plane.HORIZONTAL, relativeBlocks, localDimension, localPosition, localRotation),
        END_PORTAL_CONFIG
)

class EndEntryPortalEntity(
        world: World,
        localDimension: Int, localPosition: BlockPos, localRotation: Rotation
) : EndPortalEntity(
        localDimension == 1, world,
        (0 until 3).flatMap { x -> (0 until 3).map { z -> BlockPos(-x, 0, -z) } }.toSet(),
        localDimension, localPosition, localRotation
) {
    @Suppress("unused")
    constructor(world: World) : this(world, 0, BlockPos.ORIGIN, Rotation.NONE)

    override val portalFrameBlock: Block
        get() = Blocks.END_PORTAL_FRAME
}

class EndExitPortalEntity(
        world: World,
        localDimension: Int, localPosition: BlockPos, localRotation: Rotation
) : EndPortalEntity(
        localDimension != 1, world,
        (-2..2).flatMap { x -> (-2..2).mapNotNull { z ->
            if (abs(x) == 2 && abs(z) == 2 || x == 0 && z == 0) null
            else BlockPos(-x-2, 0, -z-2)
        } }.toSet(),
        localDimension, localPosition, localRotation
) {
    @Suppress("unused")
    constructor(world: World) : this(world, 0, BlockPos.ORIGIN, Rotation.NONE)

    override val portalFrameBlock: Block
        get() = Blocks.BEDROCK

    /**
     * Check if the world spawn has changed and therefore if this portal should relocated when this timer reaches 0.
     */
    private val checkWorldSpawnDelay = TickTimer(60 * 20, world)

    override fun onUpdate() {
        if (!world.isRemote && !isTailEnd) {
            // Destruction of the end exit portal does not call [Block.neighborChanged], so we need to check here
            // manually for the disappearance of our portal.
            if (portal.localBlocks.any { world.getBlockState(it).block !is BlockEndPortal }) {
                setDead()
            }
        }
        if (!world.isRemote && isTailEnd) {
            checkWorldSpawnDelay.tick("checkWorldSpawn") {
                // Just update the original position, the normal tail obstruction check will take it from there.
                originalTailPos = world.getTopSolidOrLiquidBlock(world.spawnPoint).add(0, 40, 0).let {
                    if (it.y > world.height) BlockPos(it.x, world.height - 1, it.z) else it
                }
            }
        }
        super.onUpdate()
    }
}
