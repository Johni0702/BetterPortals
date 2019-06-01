package de.johni0702.minecraft.betterportals.common

import net.minecraft.block.state.IBlockState
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos
import javax.vecmath.AxisAngle4d
import javax.vecmath.Matrix4d
import javax.vecmath.Point3d
import javax.vecmath.Vector3d

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

        e1.rotationYawHead = e2.rotationYawHead.also { e2.rotationYawHead = e1.rotationYawHead }
        e1.prevRotationYawHead = e2.prevRotationYawHead.also { e2.prevRotationYawHead = e1.prevRotationYawHead }
        e1.renderYawOffset = e2.renderYawOffset.also { e2.renderYawOffset = e1.renderYawOffset }
        e1.prevRenderYawOffset = e2.prevRenderYawOffset.also { e2.prevRenderYawOffset = e1.prevRenderYawOffset }

        e1.setPosition(e1.posX, e1.posY, e1.posZ)
        e2.setPosition(e2.posX, e2.posY, e2.posZ)

        e1.motionX = e2.motionX.also { e2.motionX = e1.motionX }
        e1.motionY = e2.motionY.also { e2.motionY = e1.motionY }
        e1.motionZ = e2.motionZ.also { e2.motionZ = e1.motionZ }
    }

    fun transformPosition(from: Entity, to: Entity, portal: Portal) {
        val rotation = portal.remoteRotation - portal.localRotation
        transformPosition(from, to, portal.localToRemoteMatrix, rotation.degrees.toFloat())
    }

    fun transformPosition(from: Entity, to: Entity, matrix: Matrix4d, yawOffset: Float) {
        with(from) { matrix * Point3d(posX, posY, posZ) }.let { pos ->
            to.setPosition(pos.x, pos.y, pos.z)
        }
        with(from) { matrix * Point3d(prevPosX, prevPosY, prevPosZ) }.let { pos ->
            to.prevPosX = pos.x
            to.prevPosY = pos.y
            to.prevPosZ = pos.z
        }
        with(from) { matrix * Point3d(lastTickPosX, lastTickPosY, lastTickPosZ) }.let { pos ->
            to.lastTickPosX = pos.x
            to.lastTickPosY = pos.y
            to.lastTickPosZ = pos.z
        }
        with(from) { matrix * Vector3d(motionX, motionY, motionZ) }.let { pos ->
            to.motionX = pos.x
            to.motionY = pos.y
            to.motionZ = pos.z
        }

        to.rotationYaw = from.rotationYaw + yawOffset
        to.prevRotationYaw = from.prevRotationYaw + yawOffset
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

            to.rotationYawHead = from.rotationYawHead + yawOffset
            to.prevRotationYawHead = from.prevRotationYawHead + yawOffset
            to.renderYawOffset = from.renderYawOffset + yawOffset
            to.prevRenderYawOffset = from.prevRenderYawOffset + yawOffset
        }

        to.distanceWalkedModified = from.distanceWalkedModified
        to.prevDistanceWalkedModified = from.prevDistanceWalkedModified
        to.isSneaking = from.isSneaking
        to.isSprinting = from.isSprinting
    }
}

class Gettable<in K, out V>(
        private val getter: (K) -> V
) {
    operator fun get(key: K): V = getter(key)
    operator fun invoke(key: K) = getter(key)
}
typealias BlockCache = Gettable<BlockPos, IBlockState>

object Mat4d {
    fun id() = Matrix4d().apply { setIdentity() }
    fun add(dx: Double, dy: Double, dz: Double) = add(Vector3d(dx, dy, dz))
    fun add(vec: Vector3d) = id().apply { setTranslation(vec) }
    fun sub(dx: Double, dy: Double, dz: Double) = sub(Vector3d(dx, dy, dz))
    fun sub(vec: Vector3d) = id().apply { setTranslation(Vector3d().also { it.negate(vec) }) }
    fun rotYaw(angle: Number) = id().apply { setRotation(AxisAngle4d(0.0, -1.0, 0.0, Math.toRadians(angle.toDouble()))) }
    fun inverse(of: Matrix4d) = id().apply { invert(of) }
}
