package de.johni0702.minecraft.betterportals.common

import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.math.AxisAlignedBB

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
}
