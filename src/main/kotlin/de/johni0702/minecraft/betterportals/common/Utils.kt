package de.johni0702.minecraft.betterportals.common

import io.netty.util.ReferenceCounted
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.Rotation
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d

object Utils {
    val EMPTY_AABB = AxisAlignedBB(0.0, 0.0, 0.0, 0.0, 0.0, 0.0)

    fun swapPosRot(e1: EntityPlayer, e2: EntityPlayer) {
        e1.posX = e2.posX.also { e2.posX = e1.posX }
        e1.posY = e2.posY.also { e2.posY = e1.posY }
        e1.posZ = e2.posZ.also { e2.posZ = e1.posZ }
        e1.prevPosX = e2.prevPosX.also { e2.prevPosX = e1.prevPosX }
        e1.prevPosY = e2.prevPosY.also { e2.prevPosY = e1.prevPosY }
        e1.prevPosZ = e2.prevPosZ.also { e2.prevPosZ = e1.prevPosZ }
        e1.lastTickPosX = e2.lastTickPosX.also { e2.lastTickPosX = e1.lastTickPosX }
        e1.lastTickPosY = e2.lastTickPosY.also { e2.lastTickPosY = e1.lastTickPosY }
        e1.lastTickPosZ = e2.lastTickPosZ.also { e2.lastTickPosZ = e1.lastTickPosZ }

        e1.rotationYaw = e2.rotationYaw.also { e2.rotationYaw = e1.rotationYaw }
        e1.rotationPitch = e2.rotationPitch.also { e2.rotationPitch = e1.rotationPitch }
        e1.cameraYaw = e2.cameraYaw.also { e2.cameraYaw = e1.cameraYaw }
        e1.cameraPitch = e2.cameraPitch.also { e2.cameraPitch = e1.cameraPitch }

        e1.prevRotationYaw = e2.prevRotationYaw.also { e2.prevRotationYaw = e1.prevRotationYaw }
        e1.prevRotationPitch = e2.prevRotationPitch.also { e2.prevRotationPitch = e1.prevRotationPitch }
        e1.prevCameraYaw = e2.prevCameraYaw.also { e2.prevCameraYaw = e1.prevCameraYaw }
        e1.prevCameraPitch = e2.prevCameraPitch.also { e2.prevCameraPitch = e1.prevCameraPitch }

        e1.setPosition(e1.posX, e1.posY, e1.posZ)
        e2.setPosition(e2.posX, e2.posY, e2.posZ)

        e1.motionX = e2.motionX.also { e2.motionX = e1.motionX }
        e1.motionY = e2.motionY.also { e2.motionY = e1.motionY }
        e1.motionZ = e2.motionZ.also { e2.motionZ = e1.motionZ }
    }

    fun transformPosition(from: Entity, to: Entity, portal: Portal): Rotation {
        val rotation = portal.remoteRotation - portal.localRotation

        with(portal) {
            with(from) { Vec3d(posX, posY, posZ) }.fromLocal().toRemote().let { pos ->
                to.setPosition(pos.x, pos.y, pos.z)
            }
            with(from) { Vec3d(prevPosX, prevPosY, prevPosZ) }.fromLocal().toRemote().let { pos ->
                to.prevPosX = pos.x
                to.prevPosY = pos.y
                to.prevPosZ = pos.z
            }
            with(from) { Vec3d(lastTickPosX, lastTickPosY, lastTickPosZ) }.fromLocal().toRemote().let { pos ->
                to.lastTickPosX = pos.x
                to.lastTickPosY = pos.y
                to.lastTickPosZ = pos.z
            }
            with(from) { Vec3d(motionX, motionY, motionZ) }.rotate(rotation).let { pos ->
                to.motionX = pos.x
                to.motionY = pos.y
                to.motionZ = pos.z
            }
        }

        to.rotationYaw = from.rotationYaw + rotation.degrees
        to.prevRotationYaw = from.prevRotationYaw + rotation.degrees
        to.rotationPitch = from.rotationPitch
        to.prevRotationPitch = from.prevRotationPitch

        if (to is EntityPlayer && from is EntityPlayer) {
            to.cameraYaw = from.cameraYaw
            to.prevCameraYaw = from.prevCameraYaw
            to.cameraPitch = from.cameraPitch
            to.prevCameraPitch = from.prevCameraPitch

            // Sneaking
            to.height = from.height
        }

        if (to is EntityLivingBase && from is EntityLivingBase) {
            to.limbSwing = from.limbSwing
            to.limbSwingAmount = from.limbSwingAmount
            to.prevLimbSwingAmount = from.prevLimbSwingAmount

            to.rotationYawHead = from.rotationYawHead + rotation.degrees
            to.prevRotationYawHead = from.prevRotationYawHead + rotation.degrees
            to.renderYawOffset = from.renderYawOffset + rotation.degrees
            to.prevRenderYawOffset = from.prevRenderYawOffset + rotation.degrees
        }

        to.distanceWalkedModified = from.distanceWalkedModified
        to.prevDistanceWalkedModified = from.prevDistanceWalkedModified
        to.isSneaking = from.isSneaking
        to.isSprinting = from.isSprinting

        return rotation
    }
}

interface AReferenceCounted : ReferenceCounted {
    override fun touch(): ReferenceCounted = this
    override fun touch(hint: Any?): ReferenceCounted = this

    var refCnt: Int
    override fun refCnt(): Int = refCnt
    fun doRelease()

    override fun release(): Boolean = release(1)
    override fun release(decrement: Int): Boolean {
        if (decrement <= 0) return false
        if (refCnt <= 0) throw IllegalStateException("refCnt is already at 0")
        refCnt -= decrement
        if (refCnt <= 0) {
            doRelease()
            return true
        }
        return false
    }

    override fun retain(): ReferenceCounted = retain(1)
    override fun retain(increment: Int): ReferenceCounted {
        refCnt += increment
        return this
    }
}

class Gettable<in K, out V>(
        private val getter: (K) -> V
) {
    operator fun get(key: K): V = getter(key)
    operator fun invoke(key: K) = getter(key)
}
typealias BlockCache = Gettable<BlockPos, IBlockState>
