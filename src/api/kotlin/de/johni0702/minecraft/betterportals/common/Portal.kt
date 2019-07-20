package de.johni0702.minecraft.betterportals.common

import de.johni0702.minecraft.betterportals.common.entity.AbstractPortalEntity
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
import kotlin.properties.Delegates

/**
 * Represents an arbitrarily sized and possibly linked portal.
 *
 * Care should be take when implementing for read-access (especially to [localBoundingBox]/[remoteBoundingBox]) to be as
 * efficient as possible and, if at all possible, to not create any garbage because there will be a lot of accesses.
 *
 * Either implement these yourself (as is done in [AbstractPortalEntity]) or (preferably) use [FinitePortal.Impl].
 * This entire interface will probably be replaced with an immutable Portal class in a future version of the API.
 */
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

    val localToRemoteMatrix: Matrix4d get() =
        Mat4d.add((remotePosition.to3d() + Vec3d(0.5, 0.0, 0.5)).toJavaX()) *
                Mat4d.rotYaw((remoteRotation - localRotation).degrees) *
                Mat4d.sub((localPosition.to3d() + Vec3d(0.5, 0.0, 0.5)).toJavaX())

    val localDetailedBounds: Iterable<AxisAlignedBB>
    val remoteDetailedBounds: Iterable<AxisAlignedBB>

    val localBoundingBox: AxisAlignedBB
    val remoteBoundingBox: AxisAlignedBB

    fun isTarget(other: Portal): Boolean =
            remotePosition == other.localPosition
                    && remoteDimension == other.localDimension
                    && remoteRotation == other.localRotation

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
        fun link(other: Linkable)
    }

    interface Mutable : Linkable {
        override var plane: EnumFacing.Plane
        override var localDimension: Int
        override var localPosition: BlockPos
        override var localRotation: Rotation
        override var remoteDimension: Int?
        override var remotePosition: BlockPos
        override var remoteRotation: Rotation

        override fun link(other: Linkable) {
            this.remoteDimension = other.localDimension
            this.remotePosition = other.localPosition
            this.remoteRotation = other.localRotation
            if (!other.isTarget(this)) {
                other.link(this)
            }
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

    open class Impl(
            override var plane: EnumFacing.Plane,
            override var localDimension: Int,
            localPosition: BlockPos,
            localRotation: Rotation,
            override var remoteDimension: Int?,
            remotePosition: BlockPos,
            remoteRotation: Rotation,
            relativeBlocks: Set<BlockPos>
    ) : Mutable {
        override var localPosition: BlockPos by Delegates.observable(localPosition) { _, _, _ -> update() }
        override var localRotation: Rotation by Delegates.observable(localRotation) { _, _, _ -> update() }
        override var remotePosition: BlockPos by Delegates.observable(remotePosition) { _, _, _ -> update() }
        override var remoteRotation: Rotation by Delegates.observable(remoteRotation) { _, _, _ -> update() }
        override var relativeBlocks: Set<BlockPos> by Delegates.observable(relativeBlocks) { _, _, _ -> update() }

        private lateinit var _localBlocks: Set<BlockPos>
        private lateinit var _remoteBlocks: Set<BlockPos>

        private lateinit var _localDetailedBounds: List<AxisAlignedBB>
        private lateinit var _remoteDetailedBounds: List<AxisAlignedBB>

        private lateinit var _localBoundingBox: AxisAlignedBB
        private lateinit var _remoteBoundingBox: AxisAlignedBB

        override val localBlocks: Set<BlockPos> get() = _localBlocks
        override val remoteBlocks: Set<BlockPos> get() = _remoteBlocks

        override val localDetailedBounds: List<AxisAlignedBB> get() = _localDetailedBounds
        override val remoteDetailedBounds: List<AxisAlignedBB> get() = _remoteDetailedBounds

        override val localBoundingBox: AxisAlignedBB get() = _localBoundingBox
        override val remoteBoundingBox: AxisAlignedBB get() = _remoteBoundingBox

        init {
            update()
        }

        private fun update() {
            _localBlocks = relativeBlocks.map { it.toLocal() }.toSet()
            _remoteBlocks = relativeBlocks.map { it.toRemote() }.toSet()

            _localDetailedBounds = localBlocks.map(::AxisAlignedBB)
            _remoteDetailedBounds = remoteBlocks.map(::AxisAlignedBB)

            _localBoundingBox = localBlocks.toAxisAlignedBB()
            _remoteBoundingBox = remoteBlocks.toAxisAlignedBB()
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Impl

            if (plane != other.plane) return false
            if (localDimension != other.localDimension) return false
            if (localPosition != other.localPosition) return false
            if (localRotation != other.localRotation) return false
            if (remoteDimension != other.remoteDimension) return false
            if (remotePosition != other.remotePosition) return false
            if (remoteRotation != other.remoteRotation) return false
            if (relativeBlocks != other.relativeBlocks) return false

            return true
        }

        override fun hashCode(): Int {
            var result = plane.hashCode()
            result = 31 * result + localDimension
            result = 31 * result + localPosition.hashCode()
            result = 31 * result + localRotation.hashCode()
            result = 31 * result + (remoteDimension ?: 0)
            result = 31 * result + remotePosition.hashCode()
            result = 31 * result + remoteRotation.hashCode()
            result = 31 * result + relativeBlocks.hashCode()
            return result
        }
    }
}