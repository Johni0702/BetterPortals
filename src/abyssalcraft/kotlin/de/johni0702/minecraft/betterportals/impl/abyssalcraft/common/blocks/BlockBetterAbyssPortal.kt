//#if MC<11400
package de.johni0702.minecraft.betterportals.impl.abyssalcraft.common.blocks

import com.shinoow.abyssalcraft.api.block.ACBlocks
import com.shinoow.abyssalcraft.common.blocks.BlockACStone
import com.shinoow.abyssalcraft.common.blocks.BlockAbyssPortal
import com.shinoow.abyssalcraft.lib.ACLib
import de.johni0702.minecraft.betterportals.common.FinitePortal
import de.johni0702.minecraft.betterportals.common.block.PortalBlock
import de.johni0702.minecraft.betterportals.common.server
import de.johni0702.minecraft.betterportals.impl.abyssalcraft.common.ABYSSALCRAFT_MOD_ID
import de.johni0702.minecraft.betterportals.impl.abyssalcraft.common.EMPTY_AABB
import de.johni0702.minecraft.betterportals.impl.abyssalcraft.common.entity.AbyssPortalEntity
import net.minecraft.block.Block
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.EnumBlockRenderType
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos
import net.minecraft.world.IBlockAccess
import net.minecraft.world.World
import net.minecraft.world.WorldServer

class BlockBetterAbyssPortal(override val mod: Any) : BlockAbyssPortal(), PortalBlock<AbyssPortalEntity> {
    init {
        unlocalizedName = "abyportal"
        setRegistryName(ABYSSALCRAFT_MOD_ID, "abyportal")
        setLightLevel(1f)
    }

    override val portalBlock: IBlockState get() = this.defaultState
    override fun isPortalBlock(blockState: IBlockState): Boolean = blockState.block == this
    override val frameBlock: IBlockState
        get() = ACBlocks.stone.defaultState.withProperty(BlockACStone.TYPE, BlockACStone.EnumStoneType.ABYSSAL_STONE)
    override val frameStepsBlock: IBlockState get() = frameBlock
    override val maxPortalSize: Int = 100
    override val entityType: Class<AbyssPortalEntity> = AbyssPortalEntity::class.java

    override fun createPortalEntity(localEnd: Boolean, world: World, portal: FinitePortal): AbyssPortalEntity =
            AbyssPortalEntity(world, portal)

    override fun getRemoteWorldFor(localWorld: WorldServer, pos: BlockPos): WorldServer? {
        val server = localWorld.server
        val abyssDim = ACLib.abyssal_wasteland_id
        val remoteDim = if (localWorld.provider.dimensionType.id == abyssDim) 0 else abyssDim
        return server.getWorld(remoteDim)
    }

    override fun getRenderType(state: IBlockState): EnumBlockRenderType = EnumBlockRenderType.INVISIBLE
    override fun getBoundingBox(state: IBlockState, source: IBlockAccess, pos: BlockPos): AxisAlignedBB = EMPTY_AABB
    override fun onEntityCollidedWithBlock(worldIn: World, pos: BlockPos, state: IBlockState, entityIn: Entity) {
        if (entityIn is EntityPlayer) {
            validatePortalOrDestroy(worldIn, pos) // Convert vanilla abyss portals upon touching
        }
    }

    override fun neighborChanged(state: IBlockState, worldIn: World, pos: BlockPos, blockIn: Block, fromPos: BlockPos) {
        // This also happens to be called after the portal is built, so we can transparently upgrade it to a BP one
        validatePortalOrDestroy(worldIn, pos)
    }
}
//#endif