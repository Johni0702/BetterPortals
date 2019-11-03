package de.johni0702.minecraft.betterportals.common.entity

import de.johni0702.minecraft.betterportals.common.*
import de.johni0702.minecraft.betterportals.common.util.TickTimer
import net.minecraft.block.Block
import net.minecraft.block.material.Material
import net.minecraft.client.renderer.culling.ICamera
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.init.Blocks
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.network.datasync.DataParameter
import net.minecraft.network.datasync.DataSerializers
import net.minecraft.network.datasync.EntityDataManager
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import kotlin.math.abs

//#if MC>=11400
//$$ import net.minecraft.entity.EntityType
//$$ import net.minecraft.util.math.shapes.VoxelShape
//$$ import java.util.stream.Stream
//#endif

open class OneWayPortalEntityPortalAgent(
        manager: PortalManager,
        portalConfig: PortalConfiguration
) : PortalEntityPortalAgent(manager, portalConfig) {
    lateinit var oneWayEntity: OneWayPortalEntity
        internal set

    //#if MC>=11400
    //$$ override fun modifyAABBs(entity: Entity, queryAABB: AxisAlignedBB, vanillaStream: Stream<VoxelShape>, queryRemote: (World, AxisAlignedBB) -> Stream<VoxelShape>): Stream<VoxelShape> {
    //$$     if (oneWayEntity.isTailEnd && !oneWayEntity.isTailVisible) {
    //$$         return vanillaStream
    //$$     }
    //$$     return super.modifyAABBs(entity, queryAABB, vanillaStream, queryRemote)
    //$$ }
    //#else
    override fun modifyAABBs(entity: Entity, queryAABB: AxisAlignedBB, aabbList: MutableList<AxisAlignedBB>, queryRemote: (World, AxisAlignedBB) -> List<AxisAlignedBB>) {
        if (oneWayEntity.isTailEnd && !oneWayEntity.isTailVisible) {
            return
        }
        super.modifyAABBs(entity, queryAABB, aabbList, queryRemote)
    }
    //#endif

    override fun isInMaterial(entity: Entity, queryAABB: AxisAlignedBB, material: Material): Boolean? {
        if (oneWayEntity.isTailEnd && !oneWayEntity.isTailVisible) {
            return world.isMaterialInBB(queryAABB, material)
        }
        return super.isInMaterial(entity, queryAABB, material)
    }

    override fun checkTeleportees() {
        if (oneWayEntity.isTailEnd) return // Cannot use portal from the tail end
        super.checkTeleportees()
    }

    override fun teleportPlayer(player: EntityPlayer, from: EnumFacing): Boolean {
        val remotePortal = entity.getRemotePortal() // FIXME for some reason this call fails after the teleport; might be fixed by now
        val success = super.teleportPlayer(player, from)
        if (success) {
            (remotePortal as OneWayPortalEntity).isTailVisible = true
        }
        return success
    }

    override fun serverPortalUsed(player: EntityPlayerMP): Boolean {
        val remotePortal = entity.getRemotePortal()
        val success = super.serverPortalUsed(player)
        if (success) {
            (remotePortal as OneWayPortalEntity).isTailVisible = true
        }
        return success
    }

    override fun teleportNonPlayerEntity(entity: Entity, from: EnumFacing) {
        super.teleportNonPlayerEntity(entity, from)
        (this.entity.getRemotePortal() as OneWayPortalEntity).isTailVisible = true
    }

    override fun canBeSeen(camera: ICamera): Boolean = (!oneWayEntity.isTailEnd || oneWayEntity.isTailVisible) && super.canBeSeen(camera)
}

/**
 * A portal which really only exists at one end.
 * At the other end, it'll seem to exist while traveling through it but cannot be used to go back and disappear when
 * moving sufficiently far away.
 */
abstract class OneWayPortalEntity(
        //#if MC>=11400
        //$$ type: EntityType<out OneWayPortalEntity>,
        //#endif

        /**
         * Whether this portal instance is the tail/exit end of a pair of portals.
         * Not to be confused with the exit portal which spawns after the dragon fight; its tail end is in the overworld.
         * A pair of one-way portals cannot be entered from the tail end.
         */
        isTailEnd: Boolean,

        world: World,
        portal: FinitePortal,
        agent: OneWayPortalEntityPortalAgent
) : AbstractPortalEntity(
        //#if MC>=11400
        //$$ type,
        //#endif
        world, portal, agent
), PortalEntity.OneWay {
    constructor(
            //#if MC>=11400
            //$$ type: EntityType<out OneWayPortalEntity>,
            //#endif
            isTailEnd: Boolean, world: World, portal: FinitePortal, portalConfig: PortalConfiguration
    ) : this(
            //#if MC>=11400
            //$$ type,
            //#endif
            isTailEnd, world, portal, OneWayPortalEntityPortalAgent(world.portalManager, portalConfig)
    )

    companion object {
        private val IS_TAIL_END: DataParameter<Boolean> = EntityDataManager.createKey(OneWayPortalEntity::class.java, DataSerializers.BOOLEAN)
        private val ORIGINAL_TAIL_POS: DataParameter<BlockPos> = EntityDataManager.createKey(OneWayPortalEntity::class.java, DataSerializers.BLOCK_POS)
        private val IS_TAIL_VISIBLE: DataParameter<Boolean> = EntityDataManager.createKey(OneWayPortalEntity::class.java, DataSerializers.BOOLEAN)
    }

    override var isTailEnd: Boolean
        get() = dataManager[IS_TAIL_END]
        set(value) { dataManager[IS_TAIL_END] = value }

    var originalTailPos: BlockPos
        get() = dataManager[ORIGINAL_TAIL_POS]
        set(value) { dataManager[ORIGINAL_TAIL_POS] = value }

    var isTailVisible: Boolean
        get() = dataManager[IS_TAIL_VISIBLE]
        set(value) {
            dataManager[IS_TAIL_VISIBLE] = value
            if (value) {
                travelingInProgressTimer = 20
            }
        }

    init {
        @Suppress("LeakingThis")
        agent.oneWayEntity = this
        dataManager[IS_TAIL_END] = isTailEnd
        dataManager[ORIGINAL_TAIL_POS] = if (isTailEnd) portal.localPosition else portal.remotePosition
    }

    override fun entityInit() {
        super.entityInit()
        dataManager.register(IS_TAIL_END, false)
        dataManager.register(ORIGINAL_TAIL_POS, BlockPos.ORIGIN)
        dataManager.register(IS_TAIL_VISIBLE, false)
    }

    override fun readEntityFromNBT(compound: NBTTagCompound) {
        super.readEntityFromNBT(compound)
        with(compound.getCompoundTag("BetterPortal")) {
            isTailEnd = getBoolean("IsTailEnd")
            originalTailPos = if (hasKey("OriginalTailPos")) {
                getCompoundTag("OriginalTailPos").getXYZ()
            } else {
                if (isTailEnd) portal.localPosition else portal.remotePosition
            }
        }
    }

    override fun writeEntityToNBT(compound: NBTTagCompound) {
        super.writeEntityToNBT(compound)
        with(compound.getCompoundTag("BetterPortal")) {
            setBoolean("IsTailEnd", isTailEnd)
            setTag("OriginalTailPos", NBTTagCompound().apply {
                setXYZ(originalTailPos)
            })
        }
    }

    override var portal: FinitePortal
        get() = super.portal
        set(value) {
            // We might have previously put fake blocks in the client world and need to remove those in case the
            // portal moves
            isTailVisible = false

            super.portal = value
        }

    override fun notifyDataManagerChange(key: DataParameter<*>) {
        super.notifyDataManagerChange(key)
        if (world.isRemote && key == IS_TAIL_VISIBLE && isTailEnd) {
            val newState = (if (isTailVisible) portalFrameBlock else Blocks.AIR).defaultState
            val oldState = (if (isTailVisible) Blocks.AIR else portalFrameBlock).defaultState
            val portalBlocks = portal.localBlocks
            portalBlocks.forEach { pos ->
                EnumFacing.Plane.HORIZONTAL.forEach { facing ->
                    val neighbour = pos.offset(facing)
                    if (neighbour !in portalBlocks) {
                        if (world.getBlockState(neighbour) == oldState) {
                            world.setBlockState(neighbour, newState)
                        }
                    }
                }
            }
        }
    }

    override val isTailEndVisible: Boolean
        get() = isTailVisible
    var travelingInProgressTimer = 0

    /**
     * The type of blocks which form the fake, client-side frame at the tail end of the portal.
     */
    abstract val portalFrameBlock: Block

    /**
     * Check whether the tail end is obstructed by blocks when this timer reaches 0.
     */
    private val checkTailObstructionDelay = TickTimer(10 * 20, world)

    /**
     * Check whether there's a better (i.e. closer to [originalTailPos]) position for the tail portal when this timer
     * reaches 0.
     */
    private val checkTailPreferredPosDelay = TickTimer(60 * 20, world)

    override fun onUpdate() {
        super.onUpdate()

        if (!world.isRemote && isTailEnd) {
            if (isTailEndVisible) {
                // Check if everyone has left the portal
                val inside = world.getEntitiesWithinAABB(Entity::class.java, portal.localBoundingBox).any { entity ->
                    if (entity is OneWayPortalEntity) return@any false
                    val entityAABB = entity.entityBoundingBox
                    portal.localDetailedBounds.any { it.intersects(entityAABB) }
                }
                // and after one second, hide the portal
                if (!inside) {
                    travelingInProgressTimer--
                    if (travelingInProgressTimer <= 0) {
                        isTailVisible = false
                    }
                }
            }

            checkTailObstructionDelay.tick("checkPortalObstruction") {
                if (isObstructed()) {
                    updatePortalPosition()
                }
            }

            checkTailPreferredPosDelay.tick("findImprovedPortalPosition") {
                val currPos = portal.localPosition
                val orgPos = originalTailPos
                if (currPos != orgPos) {
                    updatePortalPosition()
                }
            }
        }
    }

    open fun isObstructed(): Boolean {
        val growVec = portal.localFacing.directionVec.to3d() * 2.0
        if (!world.isObstructed(portal.localBoundingBox.grow(growVec))) {
            return false
        }
        return portal.localDetailedBounds.any {
            world.isObstructed(it.grow(growVec))
        }
    }

    open fun updatePortalPosition() {
        val newPos = findBestUnobstructedSpace()
        if (newPos == portal.localPosition) return

        val remote = getRemotePortal() ?: return
        portal = with(portal) { FinitePortal(
                plane, blocks,
                localDimension, newPos, localRotation,
                remoteDimension, remotePosition, remoteRotation
        ) }
        remote.portal = portal.toRemote()
    }

    // This seems fairly difficult to implement well. good suggestions (and implementations) welcome.
    // Could probably do with some caching of collision boxes and better fallbacks.
    open fun findBestUnobstructedSpace(): BlockPos {
        val maxY = if (world.isCubicWorld) Int.MAX_VALUE else world.theActualHeight - 3
        val minY = if (world.isCubicWorld) Int.MIN_VALUE else 3
        val growVec = portal.localFacing.directionVec.to3d() * 2.0
        val orgPos = originalTailPos
        val orgBounds = portal.localDetailedBounds.map { it.offset((orgPos - portal.localPosition).to3d()) }

        // Check all positions close to the original position
        var bestDist = Int.MAX_VALUE
        var bestSpot: BlockPos? = null
        for (yOff in -10..10) {
            val y = orgPos.y + yOff
            if (y < minY || y > maxY) continue

            for (xOff in -10..10) {
                for (zOff in -10..10) {
                    val dist = abs(xOff) + abs(yOff) + abs(zOff)

                    if (dist >= bestDist) continue

                    val empty = orgBounds.all {
                        val bound = it.offset(xOff.toDouble(), yOff.toDouble(), zOff.toDouble())
                        !world.isObstructed(bound.grow(growVec))
                    }

                    if (empty) {
                        bestDist = dist
                        bestSpot = orgPos.add(xOff, yOff, zOff)
                    }
                }
            }
        }

        // Fallback to original position
        return bestSpot ?: orgPos
    }
}
