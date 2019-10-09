package de.johni0702.minecraft.betterportals.impl.vanilla.common.entity

import de.johni0702.minecraft.betterportals.common.DimensionId
import de.johni0702.minecraft.betterportals.common.FinitePortal
import de.johni0702.minecraft.betterportals.common.entity.OneWayPortalEntity
import de.johni0702.minecraft.betterportals.common.toDimensionId
import de.johni0702.minecraft.betterportals.impl.vanilla.common.END_PORTAL_CONFIG
import net.minecraft.block.Block
import net.minecraft.block.BlockEndPortal
import net.minecraft.init.Blocks
import net.minecraft.util.EnumFacing
import net.minecraft.util.Rotation
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import kotlin.math.abs

//#if MC>=11400
//$$ import de.johni0702.minecraft.betterportals.common.ObjectHolder
//$$ import de.johni0702.minecraft.betterportals.impl.vanilla.common.MOD_ID
//$$ import net.minecraft.util.ResourceLocation
//$$ import net.minecraft.entity.EntityType
//$$ import net.minecraftforge.registries.ForgeRegistries
//#endif

abstract class EndPortalEntity(
        //#if MC>=11400
        //$$ type: EntityType<out EndPortalEntity>,
        //#endif
        isTailEnd: Boolean, world: World, relativeBlocks: Set<BlockPos>,
        localDimension: DimensionId, localPosition: BlockPos, localRotation: Rotation
) : OneWayPortalEntity(
        //#if MC>=11400
        //$$ type,
        //#endif
        isTailEnd, world,
        FinitePortal(EnumFacing.Plane.HORIZONTAL, relativeBlocks, localDimension, localPosition, localRotation),
        END_PORTAL_CONFIG
)

class EndEntryPortalEntity(
        //#if MC>=11400
        //$$ type: EntityType<out EndPortalEntity> = ENTITY_TYPE,
        //#endif
        world: World,
        localDimension: DimensionId, localPosition: BlockPos, localRotation: Rotation
) : EndPortalEntity(
        //#if MC>=11400
        //$$ type,
        //#endif
        localDimension == 1.toDimensionId(), world,
        (0 until 3).flatMap { x -> (0 until 3).map { z -> BlockPos(-x, 0, -z) } }.toSet(),
        localDimension, localPosition, localRotation
) {
    //#if MC>=11400
    //$$ constructor(type: EntityType<EndEntryPortalEntity> = ENTITY_TYPE, world: World) : this(type, world, 0.toDimensionId()!!, BlockPos.ZERO, Rotation.NONE)
    //#else
    @Suppress("unused")
    constructor(world: World) : this(world, 0, BlockPos.ORIGIN, Rotation.NONE)
    //#endif

    override val portalFrameBlock: Block
        get() = Blocks.END_PORTAL_FRAME

    //#if MC>=11400
    //$$ companion object {
    //$$     val ID = ResourceLocation("$MOD_ID:end_entry_portal")
    //$$     val ENTITY_TYPE: EntityType<EndEntryPortalEntity> by ObjectHolder(ForgeRegistries.ENTITIES, ID)
    //$$ }
    //#endif
}

class EndExitPortalEntity(
        //#if MC>=11400
        //$$ type: EntityType<out EndExitPortalEntity> = ENTITY_TYPE,
        //#endif
        world: World,
        localDimension: DimensionId, localPosition: BlockPos, localRotation: Rotation
) : EndPortalEntity(
        //#if MC>=11400
        //$$ type,
        //#endif
        localDimension != 1.toDimensionId(), world,
        (-2..2).flatMap { x -> (-2..2).mapNotNull { z ->
            if (abs(x) == 2 && abs(z) == 2 || x == 0 && z == 0) null
            else BlockPos(-x-2, 0, -z-2)
        } }.toSet(),
        localDimension, localPosition, localRotation
) {
    //#if MC>=11400
    //$$ constructor(type: EntityType<out EndExitPortalEntity> = ENTITY_TYPE, world: World) : this(type, world, 0.toDimensionId()!!, BlockPos.ZERO, Rotation.NONE)
    //#else
    @Suppress("unused")
    constructor(world: World) : this(world, 0, BlockPos.ORIGIN, Rotation.NONE)
    //#endif

    override val portalFrameBlock: Block
        get() = Blocks.BEDROCK

    override fun onUpdate() {
        if (!world.isRemote && !isTailEnd) {
            // Destruction of the end exit portal does not call [Block.neighborChanged], so we need to check here
            // manually for the disappearance of our portal.
            if (portal.localBlocks.any { world.getBlockState(it).block !is BlockEndPortal }) {
                setDead()
            }
        }
        super.onUpdate()
    }

    //#if MC>=11400
    //$$ companion object {
    //$$     val ID = ResourceLocation("$MOD_ID:end_exit_portal")
    //$$     val ENTITY_TYPE: EntityType<EndExitPortalEntity> by ObjectHolder(ForgeRegistries.ENTITIES, ID)
    //$$ }
    //#endif
}
