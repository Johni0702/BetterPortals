package de.johni0702.minecraft.betterportals.impl.vanilla.common.blocks

import de.johni0702.minecraft.betterportals.common.dimensionId
import de.johni0702.minecraft.betterportals.common.theServer
import de.johni0702.minecraft.betterportals.common.FinitePortal
import de.johni0702.minecraft.betterportals.common.block.PortalBlock as BPPortalBlock
import de.johni0702.minecraft.betterportals.common.toDimensionId
import de.johni0702.minecraft.betterportals.impl.vanilla.common.entity.NetherPortalEntity
import net.minecraft.block.state.IBlockState
import net.minecraft.init.Blocks
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import net.minecraft.world.WorldServer

//#if MC>=11400
//#else
import de.johni0702.minecraft.betterportals.common.EMPTY_AABB
import net.minecraft.block.Block
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.world.IBlockAccess
//#endif

class BlockBetterNetherPortal(override val mod: Any) : BPPortalBlock<NetherPortalEntity> {
    override val portalBlock: IBlockState get() = Blocks.PORTAL.defaultState
    override fun isPortalBlock(blockState: IBlockState): Boolean = blockState.block == Blocks.PORTAL
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

    //#if MC>=11400
    //#else
    fun getBoundingBox(state: IBlockState, source: IBlockAccess, pos: BlockPos): AxisAlignedBB = EMPTY_AABB

    fun onEntityCollidedWithBlock(worldIn: World, pos: BlockPos, state: IBlockState, entityIn: Entity) {
        if (entityIn is EntityPlayer) {
            validatePortalOrDestroy(worldIn, pos) // Convert vanilla portals upon touching
        }
    }

    fun neighborChanged(state: IBlockState, worldIn: World, pos: BlockPos, blockIn: Block, fromPos: BlockPos) {
        validatePortalOrDestroy(worldIn, pos)
    }

    fun trySpawnPortal(localWorld: World, pos: BlockPos): Boolean = tryToLinkPortals(localWorld, pos)
    //#endif
}