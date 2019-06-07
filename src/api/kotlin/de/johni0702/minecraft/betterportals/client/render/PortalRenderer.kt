package de.johni0702.minecraft.betterportals.client.render

import de.johni0702.minecraft.betterportals.common.Portal
import de.johni0702.minecraft.betterportals.common.minus
import de.johni0702.minecraft.betterportals.common.to3dMid
import de.johni0702.minecraft.betterportals.common.toFacing
import de.johni0702.minecraft.view.client.ClientViewAPI
import de.johni0702.minecraft.view.client.render.RenderPass
import de.johni0702.minecraft.view.client.render.occlusionDetail
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.shader.Framebuffer
import net.minecraft.client.shader.ShaderManager
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.Vec3d

abstract class PortalRenderer<in P: Portal> {
    companion object {
        private val mc = Minecraft.getMinecraft()
        // Needs to be lazy as it'll fail until MC has loaded its resources (and it loads renderer before that)
        private val shader by lazy { ShaderManager(mc.resourceManager, "betterportals:render_portal") }
    }

    /**
     * Side of the portal on which the camera resides.
     *
     * Updated during [render] before any other method calls.
     */
    protected var viewFacing = EnumFacing.UP
        private set

    /**
     * Renders the given portal at the given position relative to the camera.
     */
    open fun render(portal: P, pos: Vec3d, partialTicks: Float) {
        val renderPass = ClientViewAPI.instance.getRenderPassManager(mc).current ?: return

        viewFacing = portal.localFacing.axis.toFacing(renderPass.camera.viewPosition - portal.localPosition.to3dMid())

        val parentPortal = renderPass.portalDetail?.parent
        if (parentPortal?.isTarget(portal) == true) {
            // Skip rendering of portal if it's the remote to the portal we're currently in
            return
        }

        val portalPass = renderPass.children.find {
            it.portalDetail?.parent == portal
        }

        val occlusionQuery = portalPass?.occlusionDetail?.occlusionQuery
        occlusionQuery?.begin()

        renderPortal(portal, pos, portalPass?.framebuffer, renderPass)

        occlusionQuery?.end()
    }

    protected open fun renderPortal(portal: P, pos: Vec3d, framebuffer: Framebuffer?, renderPass: RenderPass) {
        if (framebuffer == null) {
            GlStateManager.disableTexture2D()
            GlStateManager.color(0f, 0f, 0f)
        } else {
            shader.addSamplerTexture("sampler", framebuffer)
            shader.getShaderUniformOrDefault("screenSize")
                    .set(framebuffer.framebufferWidth.toFloat(), framebuffer.framebufferHeight.toFloat())
            shader.useShader()
        }
        renderPortalSurface(portal, pos, renderPass)
        if (framebuffer == null) {
            GlStateManager.color(1f, 1f, 1f)
            GlStateManager.enableTexture2D()
        } else {
            shader.endShader()
        }
    }

    protected abstract fun renderPortalSurface(portal: P, pos: Vec3d, renderPass: RenderPass)
}