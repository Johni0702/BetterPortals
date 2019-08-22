package de.johni0702.minecraft.betterportals.impl.tf.common.blocks

import de.johni0702.minecraft.betterportals.common.*
import de.johni0702.minecraft.betterportals.common.block.PortalBlock
import de.johni0702.minecraft.betterportals.impl.tf.common.LOGGER
import de.johni0702.minecraft.betterportals.impl.tf.common.TF_MOD_ID
import de.johni0702.minecraft.betterportals.impl.tf.common.entity.TFPortalEntity
import net.minecraft.block.Block
import net.minecraft.block.material.Material
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.Entity
import net.minecraft.entity.item.EntityItem
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.Blocks
import net.minecraft.network.EnumPacketDirection
import net.minecraft.network.NetHandlerPlayServer
import net.minecraft.network.NetworkManager
import net.minecraft.util.EnumFacing
import net.minecraft.util.Rotation
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import net.minecraft.world.WorldServer
import net.minecraftforge.common.util.FakePlayerFactory
import twilightforest.TFConfig
import twilightforest.TFTeleporter
import twilightforest.block.BlockTFPortal

class BlockBetterTFPortal(override val mod: Any) : BlockTFPortal(), PortalBlock<TFPortalEntity> {
    init {
        unlocalizedName = "TFPortal"
        setRegistryName(TF_MOD_ID, "twilight_portal")
        setLightLevel(1f)
    }

    override val portalBlock: Block
        get() = this
    override val frameBlock: Block
        get() = Blocks.GRASS
    override fun isFrameBlock(blockState: IBlockState): Boolean =
            blockState.isFullCube && blockState.material.let { it == Material.GRASS || it == Material.GROUND }
    override val frameStepsBlock: Block
        get() = Blocks.GRASS
    override val maxPortalSize: Int
        get() = 100
    override val entityType: Class<TFPortalEntity>
        get() = TFPortalEntity::class.java

    override fun getRemoteWorldFor(localWorld: WorldServer, pos: BlockPos): WorldServer {
        val tfDimId = TFConfig.dimension.dimensionID
        return localWorld.server.getWorld(if (localWorld.provider.dimension == tfDimId) 0 else tfDimId)
    }

    override fun createPortalEntity(localEnd: Boolean, world: World, plane: EnumFacing.Plane, portalBlocks: Set<BlockPos>, localDim: Int, localPos: BlockPos, localRot: Rotation): TFPortalEntity =
            TFPortalEntity(!localEnd, world, portalBlocks, localDim, localPos, localRot, null, BlockPos.ORIGIN, Rotation.NONE)

    private fun makeBetterPortal(localWorld: WorldServer, pos: BlockPos) {
        val localBlocks = findPortalFrame(localWorld.makeBlockCache(), pos, EnumFacing.Axis.Y, true)
        if (localBlocks.isEmpty()) {
            LOGGER.warn("Couldn't find TF portal at $pos even though it was just created?")
            return
        }

        val localDim = localWorld.provider.dimension
        val localPos = localBlocks.minByAnyCoord()!!
        val rot = Rotation.NONE
        val portalBlocks = localBlocks.mapTo(mutableSetOf()) { it.subtract(localPos).rotate(rot.reverse) }

        if (localWorld.getEntitiesWithinAABB(TFPortalEntity::class.java, AxisAlignedBB(localPos)).isNotEmpty()) {
            return // That portal's already linked
        }

        val remoteWorld = getRemoteWorldFor(localWorld, pos)
        val remoteDim = remoteWorld.provider.dimension

        // Let TF determine the optimal target position (and spawn a return portal if necessary)
        val teleporter = TFTeleporter.getTeleporterForDim(remoteWorld.server, remoteWorld.provider.dimension)
        val fakePlayer = FakePlayerFactory.getMinecraft(localWorld)
        fakePlayer.pos = localPos.to3d()
        fakePlayer.connection = fakePlayer.connection.also {
            fakePlayer.connection = NetHandlerPlayServer(localWorld.server, NetworkManager(EnumPacketDirection.CLIENTBOUND), fakePlayer)
            teleporter.placeInPortal(fakePlayer, 0f)
        }
        // TF places us somewhere randomly close to the exit portal (one block above the portal)
        val targetPos = BlockPos(fakePlayer.pos).down(1)
        // It also clears five blocks above that portal, so let's just put our tail end up there
        val remotePos = targetPos.up(5)

        val localPortal = createPortalEntity(true, localWorld, EnumFacing.Plane.HORIZONTAL, portalBlocks, localDim, localPos, rot)
        localPortal.localBlocks.forEach {
            localWorld.setBlockState(it, portalBlock.defaultState, 2)
        }
        localWorld.forceSpawnEntity(localPortal)

        val remotePortal = createPortalEntity(false, remoteWorld, EnumFacing.Plane.HORIZONTAL, portalBlocks, remoteDim, remotePos, rot)
        remoteWorld.forceSpawnEntity(remotePortal)

        localPortal.link(remotePortal)

        // Now, TF has already created the exit (or entry, really depends on your perspective) portal for us,
        // time to find it and recursively upgrade it to a better one
        // It should be somewhere around here
        for (xOff in -5..5) {
            for (zOff in -5..5) {
                val portalPos = targetPos.add(xOff, 0, zOff)
                if (remoteWorld.getBlockState(portalPos) == defaultState) {
                    // Found it!
                    makeBetterPortal(remoteWorld, portalPos)
                    return
                }
            }
        }
    }

    override fun tryToCreatePortal(world: World, pos: BlockPos, catalyst: EntityItem, player: EntityPlayer?): Boolean {
        val success = super.tryToCreatePortal(world, pos, catalyst, player)
        if (success && world is WorldServer) makeBetterPortal(world, pos)
        return success
    }

    override fun neighborChanged(state: IBlockState, worldIn: World, pos: BlockPos, blockIn: Block, fromPos: BlockPos) {
        @Suppress("DEPRECATION")
        super.neighborChanged(state, worldIn, pos, blockIn, fromPos)
        if (worldIn.getBlockState(pos).block != this) {
            worldIn.getEntitiesWithinAABB(TFPortalEntity::class.java, AxisAlignedBB(pos)).forEach {
                it.setDead()
            }
        }
    }

    override fun addCollisionBoxToList(state: IBlockState, worldIn: World, pos: BlockPos, entityBox: AxisAlignedBB, collidingBoxes: MutableList<AxisAlignedBB>, entityIn: Entity?, isActualState: Boolean) {
        addCollisionBoxToList(pos, entityBox, collidingBoxes, state.getCollisionBoundingBox(worldIn, pos))
    }

    private fun hasPortal(worldIn: World, pos: BlockPos): Boolean {
        val portalPos = findPortalFrame(worldIn.makeBlockCache(), pos, EnumFacing.Axis.Y, true).minByAnyCoord()
        return portalPos != null && worldIn.getEntitiesWithinAABB(entityType, AxisAlignedBB(portalPos)).isNotEmpty()
    }

    override fun onEntityCollidedWithBlock(worldIn: World, pos: BlockPos, state: IBlockState, entityIn: Entity) {
        if (hasPortal(worldIn, pos)) {
            // Better portal exists for this portal block
            return
        } else if (worldIn is WorldServer){
            // Legacy portal, convert to better portal
            makeBetterPortal(worldIn, pos)
        }
    }
}