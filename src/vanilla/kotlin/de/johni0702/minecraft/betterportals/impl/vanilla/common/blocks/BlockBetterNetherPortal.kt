package de.johni0702.minecraft.betterportals.impl.vanilla.common.blocks

import de.johni0702.minecraft.betterportals.common.dimensionId
import de.johni0702.minecraft.betterportals.common.theServer
import de.johni0702.minecraft.betterportals.common.FinitePortal
import de.johni0702.minecraft.betterportals.common.block.PortalBlock as BPPortalBlock
import de.johni0702.minecraft.betterportals.common.toDimensionId
import de.johni0702.minecraft.betterportals.impl.vanilla.common.entity.NetherPortalEntity
import net.minecraft.block.BlockPortal
import net.minecraft.block.SoundType
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.Blocks
import net.minecraft.util.EnumBlockRenderType
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import net.minecraft.world.WorldServer

//#if MC>=11400
//$$ import de.johni0702.minecraft.betterportals.impl.makeBlockSettings
//$$ import net.minecraft.block.material.Material
//$$ import net.minecraft.util.Direction
//$$ import net.minecraft.util.math.shapes.ISelectionContext
//$$ import net.minecraft.util.math.shapes.VoxelShape
//$$ import net.minecraft.util.math.shapes.VoxelShapes
//$$ import net.minecraft.world.IBlockReader
//$$ import net.minecraft.world.IWorld
//#else
import de.johni0702.minecraft.betterportals.common.EMPTY_AABB
import net.minecraft.block.Block
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.world.IBlockAccess
//#endif

class BlockBetterNetherPortal(override val mod: Any) : BlockPortal(
        //#if MC>=11400
        //$$ makeBlockSettings(Material.PORTAL) {
        //$$     doesNotBlockMovement()
        //$$     tickRandomly()
        //$$     hardnessAndResistance(-1.0f, -1.0f)
        //$$     sound(SoundType.GLASS)
        //$$     lightValue(15)
        //$$     noDrops()
        //$$ }
        //#endif
), BPPortalBlock<NetherPortalEntity> {
    //#if FABRIC<=0
    init {
        //#if MC>=11400
        //$$ setRegistryName("minecraft", "nether_portal")
        //#else
        setRegistryName("minecraft", "portal")
        unlocalizedName = "portal"
        setBlockUnbreakable()
        setLightLevel(1f)
        soundType = SoundType.GLASS
        //#endif
    }
    //#endif

    override val portalBlock: IBlockState get() = this.defaultState
    override fun isPortalBlock(blockState: IBlockState): Boolean = blockState.block == this
    override val frameBlock: IBlockState get() = Blocks.OBSIDIAN.defaultState
    override val frameStepsBlock: IBlockState get() = Blocks.OBSIDIAN.defaultState
    override val maxPortalSize: Int = 100
    override val entityType: Class<NetherPortalEntity> = NetherPortalEntity::class.java

    override fun createPortalEntity(localEnd: Boolean, world: World, portal: FinitePortal): NetherPortalEntity =
            NetherPortalEntity(world = world, portal = portal)

    override fun getRemoteWorldFor(localWorld: WorldServer, pos: BlockPos): WorldServer? {
        val server = localWorld.theServer
        if (!server.allowNether) return null
        val overworldDim = 0.toDimensionId()!!
        val netherDim = (-1).toDimensionId()!!
        val remoteDim = if (localWorld.dimensionId == netherDim) overworldDim else netherDim
        return server.getWorld(remoteDim)
    }

    override fun getRenderType(state: IBlockState): EnumBlockRenderType = EnumBlockRenderType.INVISIBLE

    //#if MC>=11400
    //$$ override fun getShape(state: BlockState, worldIn: IBlockReader, pos: BlockPos, context: ISelectionContext): VoxelShape =
    //$$         VoxelShapes.empty()
    //$$
    //$$ override fun onEntityCollision(state: BlockState, worldIn: World, pos: BlockPos, entityIn: Entity) {
    //$$     if (entityIn is PlayerEntity) {
    //$$         validatePortalOrDestroy(worldIn, pos) // Convert vanilla portals upon touching
    //$$     }
    //$$ }
    //$$
    //$$ override fun updatePostPlacement(stateIn: BlockState, facing: Direction, facingState: BlockState, worldIn: IWorld, currentPos: BlockPos, facingPos: BlockPos): BlockState {
    //$$     if (worldIn is World) {
    //$$         validatePortalOrDestroy(worldIn, currentPos)
    //$$     }
    //$$     return stateIn
    //$$ }
    //$$
    //$$ override fun trySpawnPortal(localWorld: IWorld, pos: BlockPos): Boolean {
    //$$     return tryToLinkPortals(localWorld as? World ?: return false, pos)
    //$$ }
    //#else
    override fun getBoundingBox(state: IBlockState, source: IBlockAccess, pos: BlockPos): AxisAlignedBB = EMPTY_AABB

    override fun onEntityCollidedWithBlock(worldIn: World, pos: BlockPos, state: IBlockState, entityIn: Entity) {
        if (entityIn is EntityPlayer) {
            validatePortalOrDestroy(worldIn, pos) // Convert vanilla portals upon touching
        }
    }

    override fun neighborChanged(state: IBlockState, worldIn: World, pos: BlockPos, blockIn: Block, fromPos: BlockPos) {
        validatePortalOrDestroy(worldIn, pos)
    }

    override fun trySpawnPortal(localWorld: World, pos: BlockPos): Boolean = tryToLinkPortals(localWorld, pos)
    //#endif
}