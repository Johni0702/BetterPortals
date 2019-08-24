package de.johni0702.minecraft.betterportals.impl.mekanism.client.tile.renderer

import de.johni0702.minecraft.betterportals.client.render.FramedPortalRenderer
import de.johni0702.minecraft.betterportals.common.FinitePortal
import de.johni0702.minecraft.betterportals.impl.mekanism.common.tile.TileEntityBetterTeleporter
import de.johni0702.minecraft.view.client.render.RenderPass
import mekanism.api.EnumColor
import mekanism.client.render.tileentity.RenderTeleporter
import mekanism.common.tile.TileEntityTeleporter
import net.minecraft.client.shader.Framebuffer
import net.minecraft.util.ResourceLocation
import net.minecraft.util.math.Vec3d
import org.lwjgl.opengl.GL11

class RenderBetterTeleporter(private val opacity: () -> Double) : RenderTeleporter() {
    private var renderingSkipped = true
    private val portalRenderer = object : FramedPortalRenderer() {
        override fun renderPortal(portal: FinitePortal, pos: Vec3d, framebuffer: Framebuffer?, renderPass: RenderPass) {
            super.renderPortal(portal, pos, framebuffer, renderPass)
            renderingSkipped = false
        }
    }

    private var renderOpacity = 1.0

    override fun render(tileEntity: TileEntityTeleporter, x: Double, y: Double, z: Double, partialTick: Float, destroyStage: Int, alpha: Float) {
        if (tileEntity is TileEntityBetterTeleporter && tileEntity.active) {
            val agent = tileEntity.agent
            if (agent != null) {
                renderingSkipped = true
                portalRenderer.render(agent.portal, Vec3d(x + 0.5, y + 1.5, z + 0.5), partialTick)
                if (renderingSkipped) {
                    return
                }
            }
        }
        val opacity = opacity()
        if (opacity > 0) {
            renderOpacity = opacity
            super.render(tileEntity, x, y, z, partialTick, destroyStage, alpha)
        }
    }

    // Simple hook to overwrite alpha depending on portal opacity setting
    override fun bindTexture(location: ResourceLocation) {
        val alpha = 0.75f * renderOpacity.toFloat()
        with(EnumColor.PURPLE) { GL11.glColor4f(getColor(0), getColor(1), getColor(2), alpha) }
        super.bindTexture(location)
    }
}