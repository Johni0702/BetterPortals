package de.johni0702.minecraft.betterportals.client.renderer

import de.johni0702.minecraft.betterportals.BetterPortalsMod.Companion.viewManager
import de.johni0702.minecraft.betterportals.client.UtilsClient
import de.johni0702.minecraft.betterportals.client.glMask
import de.johni0702.minecraft.betterportals.client.renderFullScreen
import de.johni0702.minecraft.betterportals.client.view.ClientView
import de.johni0702.minecraft.betterportals.common.*
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.client.shader.ShaderManager
import net.minecraftforge.client.ForgeHooksClient
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import org.lwjgl.opengl.GL11
import java.time.Duration

class TransferToDimensionRenderer(
        val fromView: ClientView,
        val toView: ClientView,
        private val whenDone: () -> Unit,
        val duration: Duration = Duration.ofSeconds(10)
) {
    private val mc = Minecraft.getMinecraft()

    private val shader = ShaderManager(mc.resourceManager, "betterportals:dimension_transition")
    private val eventHandler = EventHandler()

    private val cameraYawOffset = fromView.camera.rotationYaw - toView.camera.rotationYaw
    private val cameraPosOffset =
            Mat4d.add(fromView.camera.pos.toJavaX()) * Mat4d.rotYaw(cameraYawOffset) * Mat4d.sub(toView.camera.pos.toJavaX())
    private var ticksPassed = 0

    init {
        // Copy current pitch onto new camera to prevent sudden camera jumps when switching views
        UtilsClient.transformPosition(fromView.camera, toView.camera, Mat4d.inverse(cameraPosOffset), -cameraYawOffset)
    }

    private fun getProgress(partialTicks: Float) = ((ticksPassed + partialTicks) * 50 / duration.toMillis()).coerceIn(0f, 1f)

    init {
        eventHandler.registered = true
    }

    private fun computeStencilBuffer(partialTicks: Float) {
        shader.getShaderUniformOrDefault("progress").set(getProgress(partialTicks))
        shader.useShader()

        val tessellator = Tessellator.getInstance()
        tessellator.buffer.apply {
            begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION)
            pos( 1.0,  1.0,  1.0).endVertex()
            pos(-1.0,  1.0,  1.0).endVertex()
            pos(-1.0, -1.0,  1.0).endVertex()
            pos( 1.0, -1.0,  1.0).endVertex()
        }
        tessellator.draw()

        shader.endShader()
    }

    private fun renderTransition(partialTicks: Float) {
        UtilsClient.transformPosition(toView.camera, fromView.camera, cameraPosOffset, cameraYawOffset)

        GlStateManager.pushMatrix()
        GlStateManager.pushAttrib()

        GlStateManager.disableTexture2D()

        // Step one, compute stencil buffer via shader
        glMask(false, false, false, false, false, 0xff)
        GlStateManager.disableDepth()
        GL11.glEnable(GL11.GL_STENCIL_TEST)
        GL11.glClearStencil(0x00)
        GL11.glClear(GL11.GL_STENCIL_BUFFER_BIT)
        GL11.glStencilFunc(GL11.GL_ALWAYS, 0xff, 0xff)
        GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_REPLACE)
        computeStencilBuffer(partialTicks)

        GL11.glStencilFunc(GL11.GL_EQUAL, 0xff, 0xff)
        GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP)
        GlStateManager.enableDepth()

        fromView.withView {
            // Step two, reset depth buffer (and color sky) where stencil buffer is marked
            glMask(true, true, true, true, true, 0x00)
            GlStateManager.depthFunc(GL11.GL_ALWAYS)
            GL11.glDepthRange(1.0, 1.0) // any depth is 1

            GlStateManager.disableFog()
            GlStateManager.disableLighting()
            mc.entityRenderer.disableLightmap()
            mc.entityRenderer.updateFogColor(partialTicks)
            with(GlStateManager.clearState.color) { GlStateManager.color(red, green, blue) }

            glMask(true, true, true, false, true, 0x00)
            renderFullScreen()
            GL11.glDepthRange(0.0, 1.0)
            GlStateManager.depthFunc(GL11.GL_LESS)

            // Step three, draw view where stencil buffer is marked
            glMask(true, true, true, true, true, 0x00)

            GlStateManager.enableTexture2D()

            val prevPlan = ViewRenderPlan.CURRENT!!
            ViewRenderPlan.CURRENT = ViewRenderPlan(ViewRenderManager.INSTANCE, null, fromView, prevPlan.camera, fromView.camera.pos, fromView.camera.cameraYaw, 0)

            mc.world.profiler.startSection("renderView" + fromView.id)

            mc.entityRenderer.renderWorld(partialTicks, System.nanoTime())

            mc.world.profiler.endSection()

            ViewRenderPlan.CURRENT = prevPlan
        }

        GlStateManager.disableTexture2D()

        // Recover from that
        ForgeHooksClient.setRenderPass(0)

        GlStateManager.popAttrib()
        GlStateManager.popMatrix()
    }

    private inner class EventHandler {
        var registered by MinecraftForge.EVENT_BUS

        @SubscribeEvent
        fun preClientTick(event: TickEvent.ClientTickEvent) {
            if (event.phase != TickEvent.Phase.START) return
            ticksPassed++
        }

        @SubscribeEvent(priority = EventPriority.LOW)
        fun onRenderWorldLast(event: RenderWorldLastEvent) {
            if (viewManager.activeView == viewManager.mainView && viewManager.activeView == toView) {
                renderTransition(event.partialTicks)
            }

            if (getProgress(event.partialTicks) >= 1) {
                registered = false
                shader.deleteShader()
                whenDone()
            }
        }
    }
}