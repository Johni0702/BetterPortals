package de.johni0702.minecraft.betterportals.impl.vanilla.common.blocks

import de.johni0702.minecraft.betterportals.common.add
import de.johni0702.minecraft.betterportals.common.dimensionId
import de.johni0702.minecraft.betterportals.common.theServer
import de.johni0702.minecraft.betterportals.common.toDimensionId
import de.johni0702.minecraft.betterportals.impl.vanilla.common.entity.EndEntryPortalEntity
import de.johni0702.minecraft.betterportals.impl.vanilla.common.entity.EndExitPortalEntity
import de.johni0702.minecraft.betterportals.impl.vanilla.common.entity.EndPortalEntity
import net.minecraft.block.BlockEndPortal
import net.minecraft.block.BlockEndPortalFrame
import net.minecraft.block.material.Material
import net.minecraft.block.state.BlockWorldState
import net.minecraft.block.state.IBlockState
import net.minecraft.block.state.pattern.BlockMatcher
import net.minecraft.block.state.pattern.BlockPattern
import net.minecraft.block.state.pattern.FactoryBlockPattern
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.Blocks
import net.minecraft.tileentity.TileEntity
import net.minecraft.tileentity.TileEntityEndPortal
import net.minecraft.util.EnumBlockRenderType
import net.minecraft.util.EnumFacing
import net.minecraft.util.Rotation
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import net.minecraft.world.WorldServer

//#if MC>=11400
//$$ import de.johni0702.minecraft.betterportals.impl.makeBlockSettings
//$$ import net.minecraft.block.material.MaterialColor
//$$ import net.minecraft.util.math.shapes.ISelectionContext
//$$ import net.minecraft.util.math.shapes.VoxelShape
//$$ import net.minecraft.util.math.shapes.VoxelShapes
//$$ import net.minecraft.world.IBlockReader
//$$ import net.minecraft.world.IWorld
//$$ import net.minecraft.world.gen.Heightmap
//#else
import de.johni0702.minecraft.betterportals.common.EMPTY_AABB
import net.minecraft.block.Block
import net.minecraft.world.IBlockAccess
//#endif

class BlockBetterEndPortal : BlockEndPortal(
        //#if MC>=11400
        //$$ makeBlockSettings(Material.PORTAL, MaterialColor.BLACK) {
        //$$     doesNotBlockMovement()
        //$$     lightValue(15)
        //$$     hardnessAndResistance(-1.0f, 3600000.0f)
        //$$     noDrops()
        //$$ }
        //#else
        Material.PORTAL
        //#endif
) {
    companion object {
        val BETTER_END_SPAWN = BlockPos(1, 0, 1)
        val exitPattern: BlockPattern = FactoryBlockPattern.start()
                .aisle("       ", "       ", "       ", "   #   ", "       ", "       ", "       ")
                .aisle("       ", "       ", "       ", "   #   ", "       ", "       ", "       ")
                .aisle("       ", "       ", "       ", "   #   ", "       ", "       ", "       ")
                .aisle("  ###  ", " #---# ", "#-----#", "#--#--#", "#-----#", " #---# ", "  ###  ")
                .aisle("       ", "  ###  ", " ##### ", " ##### ", " ##### ", "  ###  ", "       ")
                .where('#', BlockWorldState.hasState(BlockMatcher.forBlock(Blocks.BEDROCK)))
                .where('-', { true })
                .build()
    }

    //#if FABRIC<=0
    init {
        setRegistryName("minecraft", "end_portal")
        //#if MC<11400
        unlocalizedName = "end_portal"
        setBlockUnbreakable()
        setLightLevel(1f)
        setResistance(6000000f)
        //#endif
    }
    //#endif

    override fun getRenderType(state: IBlockState): EnumBlockRenderType = EnumBlockRenderType.INVISIBLE

    //#if MC>=11400
    //$$ override fun createNewTileEntity(worldIn: IBlockReader): TileEntity = TileEntityBetterEndPortal()
    //$$ override fun getShape(state: BlockState, worldIn: IBlockReader, pos: BlockPos, context: ISelectionContext): VoxelShape =
    //$$         VoxelShapes.empty()
    //$$ override fun onEntityCollision(state: BlockState, worldIn: World, pos: BlockPos, entityIn: Entity) {
    //$$     if (entityIn is PlayerEntity) {
    //$$         makePortal(worldIn, pos) // Convert vanilla portals upon touching
    //$$     }
    //$$ }
    //$$
    //$$ override fun updatePostPlacement(stateIn: BlockState, facing: Direction, facingState: BlockState, worldIn: IWorld, pos: BlockPos, facingPos: BlockPos): BlockState {
    //$$     return if (EndPortalFrameBlock.getOrCreatePortalShape().match(worldIn, pos) == null
    //$$             && exitPattern.match(worldIn, pos) == null) {
    //$$         worldIn.getEntitiesWithinAABB(EndPortalEntity::class.java, AxisAlignedBB(pos)).forEach {
    //$$             it.remove()
    //$$         }
    //$$         Blocks.AIR.defaultState
    //$$     } else {
    //$$         stateIn
    //$$     }
    //$$ }
    //#else
    override fun createNewTileEntity(worldIn: World, meta: Int): TileEntity = TileEntityBetterEndPortal()
    override fun getBoundingBox(state: IBlockState, source: IBlockAccess, pos: BlockPos): AxisAlignedBB = EMPTY_AABB
    override fun onEntityCollidedWithBlock(worldIn: World, pos: BlockPos, state: IBlockState, entityIn: Entity) {
        if (entityIn is EntityPlayer) {
            onBlockAdded(worldIn, pos, state) // Convert vanilla portals upon touching
        }
    }

    override fun onBlockAdded(localWorld: World, pos: BlockPos, state: IBlockState) {
        makePortal(localWorld, pos)
    }

    override fun neighborChanged(state: IBlockState, worldIn: World, pos: BlockPos, blockIn: Block, fromPos: BlockPos) {
        if (BlockEndPortalFrame.getOrCreatePortalShape().match(worldIn, pos) == null
                && exitPattern.match(worldIn, pos) == null) {
            worldIn.getEntitiesWithinAABB(EndPortalEntity::class.java, AxisAlignedBB(pos)).forEach {
                it.setDead()
            }
            worldIn.setBlockState(pos, Blocks.AIR.defaultState)
        }
    }
    //#endif

    private fun makePortal(localWorld: World, pos: BlockPos) {
        if (localWorld !is WorldServer) return
        val server = localWorld.theServer

        BlockEndPortalFrame.getOrCreatePortalShape().match(localWorld, pos)?.let { pattern ->
            val localDim = localWorld.dimensionId
            val localPos = pattern.frontTopLeft.add(-1, 0, -1)
            val localRot = Rotation.NONE

            if (localWorld.getEntitiesWithinAABB(EndPortalEntity::class.java, AxisAlignedBB(localPos)).isNotEmpty()) return

            val remoteDim = 1.toDimensionId()!!
            val remoteWorld = server.getWorld(remoteDim)
            val remotePos = remoteWorld.getTopSolidOrLiquidBlock(BETTER_END_SPAWN).add(0, 40, 0).let {
                if (it.y > remoteWorld.height) BlockPos(it.x, remoteWorld.height - 1, it.z) else it
            }
            val remoteRot = Rotation.NONE

            val localPortal = EndEntryPortalEntity(
                    world = localWorld,
                    localDimension = localDim,
                    localPosition = localPos,
                    localRotation = localRot
            )
            val remotePortal = EndEntryPortalEntity(
                    world = remoteWorld,
                    localDimension = remoteDim,
                    localPosition = remotePos,
                    localRotation = remoteRot
            )

            localWorld.add(localPortal)
            remoteWorld.add(remotePortal)

            localPortal.link(remotePortal)
            return
        }
        exitPattern.match(localWorld, pos)?.let { pattern ->
            val localDim = localWorld.dimensionId
            val localPos = pattern.frontTopLeft.add(-1, -3, -1)
            val localRot = Rotation.NONE

            if (localWorld.getEntitiesWithinAABB(EndPortalEntity::class.java, AxisAlignedBB(localPos)).isNotEmpty()) return

            val remoteDim = 0.toDimensionId()!!
            val remoteWorld = server.getWorld(remoteDim)
            val remotePos = remoteWorld.getTopSolidOrLiquidBlock(remoteWorld.spawnPoint).add(0, 40, 0).let {
                if (it.y > remoteWorld.height) BlockPos(it.x, remoteWorld.height - 1, it.z) else it
            }
            val remoteRot = Rotation.NONE

            val localPortal = EndExitPortalEntity(
                    world = localWorld,
                    localDimension = localDim,
                    localPosition = localPos,
                    localRotation = localRot
            )
            val remotePortal = EndExitPortalEntity(
                    world = remoteWorld,
                    localDimension = remoteDim,
                    localPosition = remotePos,
                    localRotation = remoteRot
            )

            localWorld.add(localPortal)
            remoteWorld.add(remotePortal)

            localPortal.link(remotePortal)
            return
        }
    }
}

//#if MC>=11400
//$$ fun ServerWorld.getTopSolidOrLiquidBlock(below: BlockPos): BlockPos =
//$$         getHeight(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, below)
//#endif

class TileEntityBetterEndPortal : TileEntityEndPortal() {
    override fun shouldRenderFace(side: EnumFacing): Boolean = false
}
