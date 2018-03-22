package de.johni0702.minecraft.betterportals.client

import de.johni0702.minecraft.betterportals.common.Portal
import de.johni0702.minecraft.betterportals.common.Utils
import de.johni0702.minecraft.betterportals.common.degrees
import de.johni0702.minecraft.betterportals.common.minus
import net.minecraft.client.entity.EntityPlayerSP
import net.minecraft.client.renderer.culling.ClippingHelper
import net.minecraft.client.renderer.culling.ClippingHelperImpl

object UtilsClient {
    fun swapPosRot(e1: EntityPlayerSP, e2: EntityPlayerSP) {
        Utils.swapPosRot(e1, e2)

        e1.renderArmYaw = e2.renderArmYaw.also { e2.renderArmYaw = e1.renderArmYaw }
        e1.renderArmPitch = e2.renderArmPitch.also { e2.renderArmPitch = e1.renderArmPitch }
        e1.prevRenderArmYaw = e2.prevRenderArmYaw.also { e2.prevRenderArmYaw = e1.prevRenderArmYaw }
        e1.prevRenderArmPitch = e2.prevRenderArmPitch.also { e2.prevRenderArmPitch = e1.prevRenderArmPitch }
    }

    fun transformPosition(from: EntityPlayerSP, to: EntityPlayerSP, portal: Portal) {
        val rotation = portal.remoteRotation - portal.localRotation

        Utils.transformPosition(from, to, portal)

        to.renderArmPitch = from.renderArmPitch
        to.prevRenderArmPitch = from.prevRenderArmPitch
        to.renderArmYaw = from.renderArmYaw + rotation.degrees
        to.prevRenderArmYaw = from.prevRenderArmYaw + rotation.degrees
    }
}

// getInstance actually modifies the instance, so we have to get our reference once and hold on to it
val clippingHelper: ClippingHelper = ClippingHelperImpl.getInstance()
