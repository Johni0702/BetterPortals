//#if MC<11400
package de.johni0702.minecraft.betterportals.impl.aether.common.blocks

import com.legacy.aether.Aether
import com.legacy.aether.AetherConfig
import com.legacy.aether.blocks.portal.BlockAetherPortal
import de.johni0702.minecraft.betterportals.common.BlockCache
import de.johni0702.minecraft.betterportals.common.FinitePortal
import de.johni0702.minecraft.betterportals.common.block.PortalBlock
import de.johni0702.minecraft.betterportals.common.theServer
import de.johni0702.minecraft.betterportals.impl.aether.common.EMPTY_AABB
import de.johni0702.minecraft.betterportals.impl.aether.common.entity.AetherPortalEntity
import net.minecraft.block.Block
import net.minecraft.block.SoundType
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.Blocks
import net.minecraft.util.EnumBlockRenderType
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos
import net.minecraft.world.IBlockAccess
import net.minecraft.world.World
import net.minecraft.world.WorldServer

class BlockBetterAetherPortal(override val mod: Any) : BlockAetherPortal(), PortalBlock<AetherPortalEntity> {
    init {
        unlocalizedName = "aether_portal"
        registryName = Aether.locate("aether_portal")
        setLightLevel(1f)
        soundType = SoundType.GLASS
    }

    override val portalBlock: IBlockState get() = this.defaultState
    override fun isPortalBlock(blockState: IBlockState): Boolean = blockState.block == this
    override val frameBlock: IBlockState get() = Blocks.GLOWSTONE.defaultState
    override val frameStepsBlock: IBlockState get() = Blocks.GLOWSTONE.defaultState
    override val maxPortalSize: Int = 100
    override val entityType: Class<AetherPortalEntity> = AetherPortalEntity::class.java

    override fun createPortalEntity(localEnd: Boolean, world: World, portal: FinitePortal): AetherPortalEntity =
            AetherPortalEntity(world, portal)

    override fun getRemoteWorldFor(localWorld: WorldServer, pos: BlockPos): WorldServer? {
        val server = localWorld.theServer
        if (AetherConfig.gameplay_changes.disable_portal) return null
        val aetherDim = AetherConfig.dimension.aether_dimension_id
        val remoteDim = if (localWorld.provider.dimensionType.id == aetherDim) 0 else aetherDim
        return server.getWorld(remoteDim)
    }

    override fun considerPlacingPortalAt(blockCache: BlockCache, portalBlocks: Set<BlockPos>, pos: BlockPos, axis: EnumFacing.Axis): Boolean {
        if (pos.y < 16) {
            // Don't place any portals on y < 16 because the clouds are at that level and clouds in portals look really
            // ugly. This will also prevent placing new portals in the overworld below 16 (existing ones will still
            // connect) but that's a sacrifice I'm willing to make.
            return false
        }
        return super.considerPlacingPortalAt(blockCache, portalBlocks, pos, axis)
    }

    override fun getRenderType(state: IBlockState): EnumBlockRenderType = EnumBlockRenderType.INVISIBLE
    override fun getBoundingBox(state: IBlockState, source: IBlockAccess, pos: BlockPos): AxisAlignedBB = EMPTY_AABB
    override fun onEntityCollidedWithBlock(worldIn: World, pos: BlockPos, state: IBlockState, entityIn: Entity) {
        if (entityIn is EntityPlayer) {
            validatePortalOrDestroy(worldIn, pos) // Convert vanilla aether portals upon touching
        }
    }

    override fun neighborChanged(state: IBlockState, worldIn: World, pos: BlockPos, blockIn: Block, fromPos: BlockPos) {
        validatePortalOrDestroy(worldIn, pos)
    }

    override fun trySpawnPortal(localWorld: World, pos: BlockPos): Boolean = tryToLinkPortals(localWorld, pos)
}
//#endif
