package de.johni0702.minecraft.betterportals.common.entity

import de.johni0702.minecraft.betterportals.common.*
import net.minecraft.block.Block
import net.minecraft.client.entity.EntityPlayerSP
import net.minecraft.client.renderer.culling.ICamera
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.Blocks
import net.minecraft.nbt.NBTBase
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.EnumFacing
import net.minecraft.util.Rotation
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

open class OneWayPortalEntityPortalAgent<out E: OneWayPortalEntity>(
        manager: PortalManager,
        entity: E,
        portalConfig: PortalConfiguration
) : PortalEntityPortalAgent<E>(manager, entity, portalConfig) {

    @Deprecated("missing `PortalConfig` argument")
    constructor(manager: PortalManager, entity: E) : this(manager, entity, PortalConfiguration())

    override fun checkTeleportees() {
        if (entity.isTailEnd) return // Cannot use portal from the tail end
        super.checkTeleportees()
    }

    override fun teleportPlayer(player: EntityPlayer, from: EnumFacing): Boolean {
        val remotePortal = entity.getRemotePortal() // FIXME for some reason this call fails after the teleport; might be fixed by now
        val success = super.teleportPlayer(player, from)
        if (success) {
            (remotePortal as OneWayPortalEntity).isTravelingInProgress = true
        }
        return success
    }

    override fun canBeSeen(camera: ICamera): Boolean = (!entity.isTailEnd || entity.isTravelingInProgress) && super.canBeSeen(camera)
}

/**
 * A portal which really only exists at one end.
 * At the other end, it'll seem to exist while traveling through it but cannot be used to go back and disappear when
 * moving sufficiently far away.
 */
abstract class OneWayPortalEntity(
        /**
         * Whether this portal instance is the tail/exit end of a pair of portals.
         * Not to be confused with the exit portal which spawns after the dragon fight; its tail end is in the overworld.
         * A pair of one-way portals cannot be entered from the tail end.
         */
        isTailEnd: Boolean,

        world: World, plane: EnumFacing.Plane, relativeBlocks: Set<BlockPos>,
        localDimension: Int, localPosition: BlockPos, localRotation: Rotation,
        remoteDimension: Int?, remotePosition: BlockPos, remoteRotation: Rotation,
        portalConfig: PortalConfiguration
) : AbstractPortalEntity(
        world, plane, relativeBlocks,
        localDimension, localPosition, localRotation,
        remoteDimension,remotePosition, remoteRotation,
        portalConfig
), PortalEntity.OneWay<FinitePortal.Mutable> {

    override var isTailEnd: Boolean = isTailEnd
        set(value) {
            field = value
            // FIXME should be superfluous once [Portal] is no longer an interface
            dataManager[PORTAL] = writePortalToNBT()
        }

    @Deprecated("missing `PortalConfig` argument")
    constructor(
            isTailEnd: Boolean,
            world: World, plane: EnumFacing.Plane, relativeBlocks: Set<BlockPos>,
            localDimension: Int, localPosition: BlockPos, localRotation: Rotation,
            remoteDimension: Int?, remotePosition: BlockPos, remoteRotation: Rotation
    ) : this(isTailEnd, world, plane, relativeBlocks, localDimension, localPosition, localRotation, remoteDimension, remotePosition, remoteRotation,
            PortalConfiguration())

    override val agent = OneWayPortalEntityPortalAgent(world.portalManager, this, portalConfig)

    init {
        // Update PORTAL data tracker to include isTailEnd
        // FIXME should be superfluous once [Portal] is no longer an interface
        @Suppress("LeakingThis")
        dataManager[PORTAL] = writePortalToNBT()
    }

    override fun writePortalToNBT(): NBTTagCompound =
            super.writePortalToNBT().apply { setBoolean("IsTailEnd", isTailEnd) }

    override fun readPortalFromNBT(nbt: NBTBase?) {
        super.readPortalFromNBT(nbt)
        (nbt as? NBTTagCompound)?.apply {
            isTailEnd = getBoolean("IsTailEnd")
        }
    }

    /**
     * When the player has just passed through the portal, the other end will still be rendered while the player
     * hasn't moved away from it.
     * This is to prevent the portal from disappearing off of half of the screen.
     */
    var isTravelingInProgress = false
        set(value) {
            if (field == value) return
            field = value
            val newState = (if (value) portalFrameBlock else Blocks.AIR).defaultState
            val oldState = (if (value) Blocks.AIR else portalFrameBlock).defaultState
            val portalBlocks = localBlocks
            portalBlocks.forEach { pos ->
                EnumFacing.HORIZONTALS.forEach { facing ->
                    val neighbour = pos.offset(facing)
                    if (neighbour !in portalBlocks) {
                        if (world.getBlockState(neighbour) == oldState) {
                            world.setBlockState(neighbour, newState)
                        }
                    }
                }
            }
            if (value && travelingInProgressTimer == 0) {
                travelingInProgressTimer = 20
            }
        }
    override val isTailEndVisible: Boolean
        get() = isTravelingInProgress
    var travelingInProgressTimer = 0

    /**
     * The type of blocks which form the fake, client-side frame at the tail end of the portal.
     */
    abstract val portalFrameBlock: Block

    override fun onClientUpdate() {
        super.onClientUpdate()

        if (isTravelingInProgress && isTailEnd) {
            // Check whether the player has moved away from the tail end of the portal far enough so we can hide it
            val nearby = world.playerEntities.filterIsInstance<EntityPlayerSP>().any {
                // Traveling is still considered in progress if the distance to the portal center is less than 10 blocks
                localBoundingBox.center.squareDistanceTo(it.pos) < 100.0
            }

            // or they're no longer inside of it and enough time has passed, e.g. if they're standing next to it
            val inside = world.playerEntities.filterIsInstance<EntityPlayerSP>().any { player ->
                val playerAABB = player.entityBoundingBox
                localDetailedBounds.any { it.intersects(playerAABB) }
            }
            if (!inside && travelingInProgressTimer > 0) {
                travelingInProgressTimer--
            }

            if (!nearby || travelingInProgressTimer == 0) {
                isTravelingInProgress = false
            }
        }
    }
}
