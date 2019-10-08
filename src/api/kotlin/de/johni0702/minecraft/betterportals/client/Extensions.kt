package de.johni0702.minecraft.betterportals.client

import de.johni0702.minecraft.betterportals.common.Portal
import de.johni0702.minecraft.betterportals.common.degrees
import de.johni0702.minecraft.betterportals.common.derivePosRotFrom
import de.johni0702.minecraft.betterportals.common.hasVivecraft
import de.johni0702.minecraft.betterportals.common.minus
import de.johni0702.minecraft.betterportals.common.radians
import de.johni0702.minecraft.betterportals.common.times
import de.johni0702.minecraft.betterportals.common.toMC
import de.johni0702.minecraft.betterportals.common.toPoint
import net.minecraft.client.Minecraft
import net.minecraft.client.entity.EntityPlayerSP
import org.vivecraft.gameplay.OpenVRPlayer
import org.vivecraft.provider.MCOpenVR
import org.vivecraft.settings.VRSettings
import javax.vecmath.Matrix4d

fun EntityPlayerSP.deriveClientPosRotFrom(from: EntityPlayerSP, portal: Portal) {
    val rotation = portal.remoteRotation - portal.localRotation
    deriveClientPosRotFrom(from, portal.localToRemoteMatrix, rotation.degrees.toFloat())
}

fun EntityPlayerSP.deriveClientPosRotFrom(from: EntityPlayerSP, matrix: Matrix4d, yawOffset: Float) {
    if (hasVivecraft && this === from && Minecraft.getMinecraft().player === this) {
        // Preserve for later because setPosition (called from derivePosRotFrom) overwrites it
        // (and incorrectly so, i.e. without taking into account the relative offset rotation)
        val roomOrigin = OpenVRPlayer.get().roomOrigin

        derivePosRotFrom(from, matrix, yawOffset)

        VRSettings.inst.vrWorldRotation -= yawOffset
        MCOpenVR.seatedRot -= yawOffset
        val spaces = OpenVRPlayer.get()
        for (space in listOf(spaces.vrdata_world_pre, spaces.vrdata_world_post)) {
            space.origin = (matrix * space.origin.toPoint()).toMC()
            space.rotation_radians -= yawOffset.radians
        }
        spaces.roomOrigin = (matrix * roomOrigin.toPoint()).toMC()
    } else {
        derivePosRotFrom(from, matrix, yawOffset)
    }

    renderArmPitch = from.renderArmPitch
    prevRenderArmPitch = from.prevRenderArmPitch
    renderArmYaw = from.renderArmYaw + yawOffset
    prevRenderArmYaw = from.prevRenderArmYaw + yawOffset
}
