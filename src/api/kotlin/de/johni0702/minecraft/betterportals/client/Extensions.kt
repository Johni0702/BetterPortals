package de.johni0702.minecraft.betterportals.client

import de.johni0702.minecraft.betterportals.common.Portal
import de.johni0702.minecraft.betterportals.common.degrees
import de.johni0702.minecraft.betterportals.common.derivePosRotFrom
import de.johni0702.minecraft.betterportals.common.minus
import net.minecraft.client.entity.EntityPlayerSP
import javax.vecmath.Matrix4d

fun EntityPlayerSP.deriveClientPosRotFrom(from: EntityPlayerSP, portal: Portal) {
    val rotation = portal.remoteRotation - portal.localRotation
    deriveClientPosRotFrom(from, portal.localToRemoteMatrix, rotation.degrees.toFloat())
}

fun EntityPlayerSP.deriveClientPosRotFrom(from: EntityPlayerSP, matrix: Matrix4d, yawOffset: Float) {
    derivePosRotFrom(from, matrix, yawOffset)

    renderArmPitch = from.renderArmPitch
    prevRenderArmPitch = from.prevRenderArmPitch
    renderArmYaw = from.renderArmYaw + yawOffset
    prevRenderArmYaw = from.prevRenderArmYaw + yawOffset
}
