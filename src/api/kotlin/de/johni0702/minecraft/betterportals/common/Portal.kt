package de.johni0702.minecraft.betterportals.common

import net.minecraft.nbt.NBTTagCompound
import net.minecraft.nbt.NBTTagList
import net.minecraft.util.EnumFacing
import net.minecraft.util.Rotation
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import net.minecraftforge.common.util.Constants
import javax.vecmath.Matrix4d

/**
 * Represents an arbitrarily sized and possibly linked portal.
 *
 * Care should be take when overriding for read-access (especially to [localBoundingBox]/[remoteBoundingBox]) to be as
 * efficient as possible and, if at all possible, to not create any garbage because there will be a lot of accesses.
 */
abstract class Portal(
        val plane: EnumFacing.Plane,
        val detailedBounds: Iterable<AxisAlignedBB>,
        val localDimension: DimensionId,
        localPosition: BlockPos,
        val localRotation: Rotation,
        val remoteDimension: DimensionId?,
        remotePosition: BlockPos,
        val remoteRotation: Rotation
) {
    val localPosition: BlockPos = localPosition.toImmutable()
    val remotePosition: BlockPos = remotePosition.toImmutable()

    val localFacing: EnumFacing = when(plane) {
        EnumFacing.Plane.VERTICAL -> localRotation.facing
        EnumFacing.Plane.HORIZONTAL -> EnumFacing.UP
    }
    val remoteFacing: EnumFacing = when(plane) {
        EnumFacing.Plane.VERTICAL -> remoteRotation.facing
        EnumFacing.Plane.HORIZONTAL -> EnumFacing.UP
    }

    val localAxis: EnumFacing.Axis = localFacing.axis
    val remoteAxis: EnumFacing.Axis = remoteFacing.axis

    fun BlockPos.toRemote(): BlockPos = rotate(remoteRotation).add(remotePosition)
    fun BlockPos.toLocal(): BlockPos = rotate(localRotation).add(localPosition)
    fun BlockPos.fromRemote(): BlockPos = subtract(remotePosition).rotate(remoteRotation.reverse)
    fun BlockPos.fromLocal(): BlockPos = subtract(localPosition).rotate(localRotation.reverse)

    fun Vec3d.toSpace(pos: BlockPos, rotation: Rotation): Vec3d =
            subtract(0.5, 0.0, 0.5).rotate(rotation).addVector(0.5, 0.0, 0.5).add(pos.to3d())
    fun Vec3d.fromSpace(pos: BlockPos, rotation: Rotation): Vec3d =
            subtract(pos.to3d()).subtract(0.5, 0.0, 0.5).rotate(rotation.reverse).addVector(0.5, 0.0, 0.5)
    fun Vec3d.toRemote(): Vec3d = toSpace(remotePosition, remoteRotation)
    fun Vec3d.toLocal(): Vec3d = toSpace(localPosition, localRotation)
    fun Vec3d.fromRemote(): Vec3d = fromSpace(remotePosition, remoteRotation)
    fun Vec3d.fromLocal(): Vec3d = fromSpace(localPosition, localRotation)

    val localToRemoteMatrix: Matrix4d =
            Mat4d.add((remotePosition.to3d() + Vec3d(0.5, 0.0, 0.5)).toJavaX()) *
                    Mat4d.rotYaw((remoteRotation - localRotation).degrees) *
                    Mat4d.sub((localPosition.to3d() + Vec3d(0.5, 0.0, 0.5)).toJavaX())

    val localDetailedBounds: Iterable<AxisAlignedBB> = detailedBounds.map { it.min.toLocal().toAxisAlignedBB(it.max.toLocal()) }
    val remoteDetailedBounds: Iterable<AxisAlignedBB> = detailedBounds.map { it.min.toRemote().toAxisAlignedBB(it.max.toRemote()) }

    val localBoundingBox: AxisAlignedBB = localDetailedBounds.reduce(AxisAlignedBB::union)
    val remoteBoundingBox: AxisAlignedBB = remoteDetailedBounds.reduce(AxisAlignedBB::union)

    open fun isTarget(other: Portal): Boolean =
            remotePosition == other.localPosition
                    && remoteDimension == other.localDimension
                    && remoteRotation == other.localRotation

    open fun writePortalToNBT(): NBTTagCompound = NBTTagCompound().apply {
        setInteger("Plane", plane.ordinal)
        setTag("Local", NBTTagCompound().apply {
            setXYZ(localPosition)
            setInteger("Rotation", localRotation.ordinal)
            setInteger("Dim", localDimension.toIntId())
        })
        val remoteDimension = this@Portal.remoteDimension
        if (remoteDimension != null) {
            setTag("Remote", NBTTagCompound().apply {
                setXYZ(remotePosition)
                setInteger("Rotation", remoteRotation.ordinal)
                setInteger("Dim", remoteDimension.toIntId())
            })
        }
    }

    constructor(detailedBounds: Iterable<AxisAlignedBB>, nbt: NBTTagCompound) : this(
            detailedBounds,
            plane = EnumFacing.Plane.values()[nbt.getInteger("Plane")],
            local = nbt.getCompoundTag("Local"),
            remote = if (nbt.hasKey("Remote")) nbt.getCompoundTag("Remote") else null
    )

    private constructor(detailedBounds: Iterable<AxisAlignedBB>, plane: EnumFacing.Plane, local: NBTTagCompound, remote: NBTTagCompound?) : this(
            plane,
            detailedBounds,
            localPosition = local.getXYZ(),
            localRotation = Rotation.values()[local.getInteger("Rotation")],
            localDimension = local.getInteger("Dim").toDimensionId()!!,
            remotePosition = remote?.getXYZ() ?: BlockPos.ORIGIN,
            remoteRotation = remote?.getInteger("Rotation")?.let { Rotation.values()[it] } ?: Rotation.NONE,
            remoteDimension = remote?.getInteger("Dim")?.toDimensionId()
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Portal

        if (plane != other.plane) return false
        if (localDimension != other.localDimension) return false
        if (localPosition != other.localPosition) return false
        if (localRotation != other.localRotation) return false
        if (remoteDimension != other.remoteDimension) return false
        if (remotePosition != other.remotePosition) return false
        if (remoteRotation != other.remoteRotation) return false
        if (detailedBounds != other.detailedBounds) return false

        return true
    }

    override fun hashCode(): Int {
        var result = plane.hashCode()
        result = 31 * result + localDimension.hashCode()
        result = 31 * result + localPosition.hashCode()
        result = 31 * result + localRotation.hashCode()
        result = 31 * result + (remoteDimension ?: 0).hashCode()
        result = 31 * result + remotePosition.hashCode()
        result = 31 * result + remoteRotation.hashCode()
        result = 31 * result + detailedBounds.hashCode()
        return result
    }
}

class FinitePortal : Portal {
    val blocks: Set<BlockPos>
    val localBlocks get() = blocks.map { it.toLocal() }.toSet()
    val remoteBlocks get() = blocks.map { it.toRemote() }.toSet()

    constructor(
            plane: EnumFacing.Plane,
            blocks: Set<BlockPos>,
            localDimension: DimensionId,
            localPosition: BlockPos,
            localRotation: Rotation,
            remoteDimension: DimensionId?,
            remotePosition: BlockPos,
            remoteRotation: Rotation
    ) : super(
            plane,
            blocks.map(::AxisAlignedBB),
            localDimension,
            localPosition,
            localRotation,
            remoteDimension,
            remotePosition,
            remoteRotation
    ) {
        this.blocks = blocks.toImmutable()
    }

    constructor(plane: EnumFacing.Plane, blocks: Set<BlockPos>, localDimension: DimensionId, localPosition: BlockPos, localRotation: Rotation)
            : this(plane, blocks, localDimension, localPosition, localRotation, null, BlockPos.ORIGIN, Rotation.NONE)

    constructor(nbt: NBTTagCompound) : this(
            nbt,
            nbt.getTagList("Blocks", Constants.NBT.TAG_COMPOUND).map {
                (it as NBTTagCompound).getXYZ()
            }.toSet()
    )
    private constructor(nbt: NBTTagCompound, blocks: Set<BlockPos>) : super(blocks.map(::AxisAlignedBB), nbt) {
        this.blocks = blocks
    }

    override fun writePortalToNBT(): NBTTagCompound = super.writePortalToNBT().apply {
        setTag("Blocks", NBTTagList().apply {
            blocks.forEach { append(NBTTagCompound().setXYZ(it)) }
        })
    }

    fun toRemote() = FinitePortal(plane, blocks, remoteDimension!!, remotePosition, remoteRotation, localDimension, localPosition, localRotation)
    fun withoutRemote() = FinitePortal(plane, blocks, localDimension, localPosition, localRotation)
    fun withRemote(other: FinitePortal): FinitePortal = FinitePortal(plane, blocks, localDimension, localPosition, localRotation, other.localDimension, other.localPosition, other.localRotation)

    companion object {
        val DUMMY = FinitePortal(EnumFacing.Plane.VERTICAL, setOf(BlockPos.ORIGIN), 0.toDimensionId()!!, BlockPos.ORIGIN, Rotation.NONE)
    }
}