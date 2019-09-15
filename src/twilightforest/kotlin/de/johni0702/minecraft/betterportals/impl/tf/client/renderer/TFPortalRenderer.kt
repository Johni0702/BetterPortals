//#if MC<11400
package de.johni0702.minecraft.betterportals.impl.tf.client.renderer

import de.johni0702.minecraft.betterportals.client.render.OneWayFramedPortalRenderer
import de.johni0702.minecraft.betterportals.common.FinitePortal
import de.johni0702.minecraft.betterportals.common.plus
import net.minecraft.client.Minecraft
import net.minecraft.util.math.Vec3d

class TFPortalRenderer(
        textureOpacity: () -> Double = { 0.0 }
) : OneWayFramedPortalRenderer(
        textureOpacity,
        { Minecraft.getMinecraft().textureMapBlocks.getAtlasSprite("minecraft:blocks/portal") }
) {
    override fun renderPortalBlocks(portal: FinitePortal, pos: Vec3d, opacity: Double) {
        super.renderPortalBlocks(portal, pos + Vec3d(0.0, 5.0 / 16.0, 0.0), opacity)
    }
}
//#endif
