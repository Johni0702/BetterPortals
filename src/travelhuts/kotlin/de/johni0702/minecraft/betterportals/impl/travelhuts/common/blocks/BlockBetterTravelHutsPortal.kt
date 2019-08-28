package de.johni0702.minecraft.betterportals.impl.travelhuts.common.blocks

import de.johni0702.minecraft.betterportals.common.FinitePortal
import de.johni0702.minecraft.betterportals.common.forceSpawnEntity
import de.johni0702.minecraft.betterportals.common.toFacing
import de.johni0702.minecraft.betterportals.common.toRotation
import de.johni0702.minecraft.betterportals.impl.travelhuts.common.TRAVELHUTS_MOD_ID
import de.johni0702.minecraft.betterportals.impl.travelhuts.common.entity.TravelHutsPortalEntity
import info.loenwind.travelhut.TravelHutMod
import info.loenwind.travelhut.blocks.BlockHutPortal
import info.loenwind.travelhut.config.Config
import net.minecraft.block.Block
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.Entity
import net.minecraft.util.BlockRenderLayer
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import kotlin.math.max

class BlockBetterTravelHutsPortal : BlockHutPortal("$TRAVELHUTS_MOD_ID:blockhutportal") {
    init {
        unlocalizedName = "blockhutportal"
        setLightLevel(1f)
    }

    private fun getBasePos(world: World, pos: BlockPos, state: IBlockState): BlockPos {
        val facing = state.getValue(FACING)
        val side = facing.rotateY().axis.toFacing(EnumFacing.AxisDirection.POSITIVE)
        var basePos = pos
        while (world.getBlockState(basePos.down()) == state) {
            basePos = basePos.down()
        }
        if (world.getBlockState(basePos.offset(side)).block == this) {
            basePos = basePos.offset(side)
        }
        return basePos
    }

    private fun getPortal(world: World, pos: BlockPos, state: IBlockState): TravelHutsPortalEntity? =
            world.getEntitiesWithinAABB(TravelHutsPortalEntity::class.java, AxisAlignedBB(getBasePos(world, pos, state))).firstOrNull()

    override fun breakBlock(world: World, pos: BlockPos, state: IBlockState) {
        val portal = getPortal(world, pos, state)
        if (portal == null) {
            for (side in EnumFacing.values()) {
                val neighbour = pos.offset(side)
                if (world.getBlockState(neighbour).block == this) {
                    world.setBlockToAir(neighbour)
                }
            }
        } else {
            portal.setDead()
        }
    }

    override fun addCollisionBoxToList(state: IBlockState, world: World, pos: BlockPos, entityBox: AxisAlignedBB, collidingBoxes: MutableList<AxisAlignedBB>, entity: Entity?, isActualState: Boolean) {
        if (getPortal(world, pos, state) == null) {
            // If we don't yet have a portal, prevent any entity from entering. Otherwise the player might walk past
            // the mid plane and get stuck inside the block once the portal opens.
            val axis = state.getValue(FACING).axis
            val aabb = if (axis == EnumFacing.Axis.X) AABB_MID_X else AABB_MID_Z
            Block.addCollisionBoxToList(pos, entityBox, collidingBoxes, aabb)
        }
        @Suppress("DEPRECATION")
        super.addCollisionBoxToList(state, world, pos, entityBox, collidingBoxes, entity, isActualState)
    }

    override fun onEntityCollidedWithBlock(world: World, pos: BlockPos, state: IBlockState, entity: Entity) {
        if (world.isRemote) {
            return
        }

        val facing = state.getValue(FACING)
        val right = facing.rotateY()
        val basePos = getBasePos(world, pos, state)

        val portal = world.getEntitiesWithinAABB(TravelHutsPortalEntity::class.java, AxisAlignedBB(basePos)).firstOrNull()
        if (portal != null) {
            return
        }

        val dim = world.provider.dimension
        val blocks = if (right.axisDirection == EnumFacing.AxisDirection.POSITIVE) PORTAL_BLOCKS_POSITIVE else PORTAL_BLOCKS_NEGATIVE
        val localPortal = FinitePortal(EnumFacing.Plane.VERTICAL, blocks, dim, basePos, facing.toRotation())

        val dstPos = findDestination(world, basePos, facing)
        if (dstPos == null) {
            var glass = world.getBlockState(basePos.offset(right, 2))
            if (glass.block != TravelHutMod.blockHutPortalGlass) {
                glass = TravelHutMod.blockHutPortalGlass.defaultState
            }
            localPortal.localBlocks.forEach {
                world.playEvent(2001, it, Block.getStateId(world.getBlockState(it)))
                world.setBlockState(it, glass)
            }
            return
        }

        val localEntity = TravelHutsPortalEntity(world, localPortal)
        val remoteEntity = TravelHutsPortalEntity(world, FinitePortal(EnumFacing.Plane.VERTICAL, blocks, dim, dstPos, facing.toRotation()))
        world.forceSpawnEntity(localEntity)
        world.forceSpawnEntity(remoteEntity)
        localEntity.link(remoteEntity)
    }

    private fun findDestination(world: World, start: BlockPos, facing: EnumFacing): BlockPos? =
            if (Config.travellingChecksInBetween.boolean) {
                (1..max(Config.generationDistance.int * 3, 10))
                        .asSequence()
                        .map { findDestination(world, start, facing, it) }
                        .firstOrNull { it != null }
            } else {
                findDestination(world, start, facing, Config.generationDistance.int)
            }

    private fun findDestination(world: World, start: BlockPos, facing: EnumFacing, offsetChunks: Int): BlockPos? {
        var target = start.offset(facing, offsetChunks * 16 - 5)

        val chunkX = target.x shr 4
        val chunkZ = target.z shr 4
        for (dx in -1..1) {
            for (dz in -1..1) {
                world.getChunkFromChunkCoords(chunkX + dx, chunkZ + dz)
            }
        }

        target = target.add(0, -target.y, 0)
        val maxY = world.actualHeight
        while (world.getBlockState(target).block != this) {
            target = target.up()
            if (target.y > maxY) {
                return null
            }
        }
        return target
    }

    override fun canRenderInLayer(state: IBlockState, layer: BlockRenderLayer): Boolean =
            layer == BlockRenderLayer.SOLID // skip transparent layer

    companion object {
        private val AABB_MID_X = AxisAlignedBB(0.4, 0.0, 0.0, 0.6, 1.0, 1.0)
        private val AABB_MID_Z = AxisAlignedBB(0.0, 0.0, 0.4, 1.0, 1.0, 0.6)


        private val PORTAL_BLOCKS_POSITIVE = setOf(
                BlockPos(0, 0, 0),
                BlockPos(1, 0, 0),
                BlockPos(0, 1, 0),
                BlockPos(1, 1, 0),
                BlockPos(0, 2, 0),
                BlockPos(1, 2, 0)
        )
        private val PORTAL_BLOCKS_NEGATIVE = PORTAL_BLOCKS_POSITIVE.map { it.offset(EnumFacing.WEST) }.toSet()
    }
}