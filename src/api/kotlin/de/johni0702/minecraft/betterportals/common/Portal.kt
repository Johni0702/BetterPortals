package de.johni0702.minecraft.betterportals.common

import net.minecraft.nbt.NBTBase
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.nbt.NBTTagList
import net.minecraft.util.EnumFacing
import net.minecraft.util.Rotation
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import net.minecraftforge.common.util.Constants
import javax.vecmath.Matrix4d

interface Portal {
    val plane: EnumFacing.Plane
    val localDimension: Int
    val localPosition: BlockPos
    val localRotation: Rotation
    val remoteDimension: Int?
    val remotePosition: BlockPos
    val remoteRotation: Rotation

    val localFacing: EnumFacing
        get() = when(plane) {
            EnumFacing.Plane.VERTICAL -> localRotation.facing
            EnumFacing.Plane.HORIZONTAL -> EnumFacing.UP
        }
    val remoteFacing: EnumFacing
        get() = when(plane) {
            EnumFacing.Plane.VERTICAL -> remoteRotation.facing
            EnumFacing.Plane.HORIZONTAL -> EnumFacing.UP
        }

    val localAxis: EnumFacing.Axis get() = localFacing.axis
    val remoteAxis: EnumFacing.Axis get() = remoteFacing.axis

    fun BlockPos.toRemote(): BlockPos = rotate(remoteRotation).add(remotePosition)
    fun BlockPos.toLocal(): BlockPos = rotate(localRotation).add(localPosition)

    fun Vec3d.toSpace(pos: BlockPos, rotation: Rotation): Vec3d =
            subtract(0.5, 0.0, 0.5).rotate(rotation).addVector(0.5, 0.0, 0.5).add(pos.to3d())
    fun Vec3d.fromSpace(pos: BlockPos, rotation: Rotation): Vec3d =
            subtract(pos.to3d()).subtract(0.5, 0.0, 0.5).rotate(rotation.reverse).addVector(0.5, 0.0, 0.5)
    fun Vec3d.toRemote(): Vec3d = toSpace(remotePosition, remoteRotation)
    fun Vec3d.toLocal(): Vec3d = toSpace(localPosition, localRotation)
    fun Vec3d.fromRemote(): Vec3d = fromSpace(remotePosition, remoteRotation)
    fun Vec3d.fromLocal(): Vec3d = fromSpace(localPosition, localRotation)

    val localToRemoteMatrix: Matrix4d get() =
        Mat4d.add((remotePosition.to3d() + Vec3d(0.5, 0.0, 0.5)).toJavaX()) *
                Mat4d.rotYaw((remoteRotation - localRotation).degrees) *
                Mat4d.sub((localPosition.to3d() + Vec3d(0.5, 0.0, 0.5)).toJavaX())

    val localDetailedBounds: Iterable<AxisAlignedBB>
    val remoteDetailedBounds: Iterable<AxisAlignedBB>

    val localBoundingBox: AxisAlignedBB
    val remoteBoundingBox: AxisAlignedBB

    fun writePortalToNBT(): NBTTagCompound = NBTTagCompound().apply {
        setInteger("Plane", plane.ordinal)
        setTag("Local", NBTTagCompound().apply {
            setXYZ(localPosition)
            setInteger("Rotation", localRotation.ordinal)
            setInteger("Dim", localDimension)
        })
        val remoteDimension = this@Portal.remoteDimension
        if (remoteDimension != null) {
            setTag("Remote", NBTTagCompound().apply {
                setXYZ(remotePosition)
                setInteger("Rotation", remoteRotation.ordinal)
                setInteger("Dim", remoteDimension)
            })
        }
    }

    interface Linkable : Portal {
        fun link(remoteDimension: Int, remotePosition: BlockPos, remoteRotation: Rotation)
    }

    interface Mutable : Linkable {
        override var plane: EnumFacing.Plane
        override var localDimension: Int
        override var localPosition: BlockPos
        override var localRotation: Rotation
        override var remoteDimension: Int?
        override var remotePosition: BlockPos
        override var remoteRotation: Rotation

        override fun link(remoteDimension: Int, remotePosition: BlockPos, remoteRotation: Rotation) {
            this.remoteDimension = remoteDimension
            this.remotePosition = remotePosition
            this.remoteRotation = remoteRotation
        }

        fun readPortalFromNBT(nbt: NBTBase?) {
            (nbt as? NBTTagCompound)?.apply {
                plane = EnumFacing.Plane.values()[getInteger("Plane")]
                getCompoundTag("Local").apply {
                    localPosition = getXYZ()
                    localRotation = Rotation.values()[getInteger("Rotation")]
                    localDimension = getInteger("Dim")
                }
                if (hasKey("Remote")) {
                    getCompoundTag("Remote").apply {
                        remotePosition = getXYZ()
                        remoteRotation = Rotation.values()[getInteger("Rotation")]
                        remoteDimension = getInteger("Dim")
                    }
                } else {
                    remotePosition = BlockPos.ORIGIN
                    remoteRotation = Rotation.NONE
                    remoteDimension = null
                }
            }
        }
    }
}

interface FinitePortal : Portal {
    val relativeBlocks: Set<BlockPos>

    val localBlocks get() = relativeBlocks.map { it.toLocal() }.toSet()
    val remoteBlocks get() = relativeBlocks.map { it.toRemote() }.toSet()

    override val localDetailedBounds get() = localBlocks.map(::AxisAlignedBB)
    override val remoteDetailedBounds get() = remoteBlocks.map(::AxisAlignedBB)

    override val localBoundingBox: AxisAlignedBB get() = localBlocks.toAxisAlignedBB()
    override val remoteBoundingBox: AxisAlignedBB get() = remoteBlocks.toAxisAlignedBB()

    override fun writePortalToNBT(): NBTTagCompound = super.writePortalToNBT().apply {
        setTag("Blocks", NBTTagList().apply {
            relativeBlocks.forEach { appendTag(NBTTagCompound().setXYZ(it)) }
        })
    }

    interface Linkable : FinitePortal, Portal.Linkable

    interface Mutable : Linkable, Portal.Mutable {
        override var relativeBlocks: Set<BlockPos>
        override fun readPortalFromNBT(nbt: NBTBase?) {
            super.readPortalFromNBT(nbt)
            (nbt as? NBTTagCompound)?.apply {
                relativeBlocks = getTagList("Blocks", Constants.NBT.TAG_COMPOUND).map {
                    (it as NBTTagCompound).getXYZ()
                }.toSet()
            }
        }
    }
}