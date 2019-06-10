package de.johni0702.minecraft.view.impl.client

import de.johni0702.minecraft.view.impl.common.swapPosRotWith
import net.minecraft.client.entity.EntityPlayerSP
import net.minecraft.client.renderer.culling.ClippingHelper
import net.minecraft.client.renderer.culling.ClippingHelperImpl

internal fun EntityPlayerSP.swapClientPosRotWith(e2: EntityPlayerSP) {
    val e1 = this

    e1.swapPosRotWith(e2)

    e1.renderArmYaw = e2.renderArmYaw.also { e2.renderArmYaw = e1.renderArmYaw }
    e1.renderArmPitch = e2.renderArmPitch.also { e2.renderArmPitch = e1.renderArmPitch }
    e1.prevRenderArmYaw = e2.prevRenderArmYaw.also { e2.prevRenderArmYaw = e1.prevRenderArmYaw }
    e1.prevRenderArmPitch = e2.prevRenderArmPitch.also { e2.prevRenderArmPitch = e1.prevRenderArmPitch }
}

// getInstance actually modifies the instance, so we have to get our reference once and hold on to it
internal val clippingHelper: ClippingHelper = ClippingHelperImpl.getInstance()
