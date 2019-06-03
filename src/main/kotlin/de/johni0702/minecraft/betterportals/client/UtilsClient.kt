package de.johni0702.minecraft.betterportals.client

import de.johni0702.minecraft.betterportals.common.*
import net.minecraft.client.Minecraft
import net.minecraft.client.entity.EntityPlayerSP
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.culling.ClippingHelper
import net.minecraft.client.renderer.culling.ClippingHelperImpl
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.util.math.Vec3d
import org.lwjgl.opengl.GL11
import java.nio.ByteBuffer
import java.nio.ByteOrder

object UtilsClient {
    fun swapPosRot(e1: EntityPlayerSP, e2: EntityPlayerSP) {
        Utils.swapPosRot(e1, e2)

        e1.renderArmYaw = e2.renderArmYaw.also { e2.renderArmYaw = e1.renderArmYaw }
        e1.renderArmPitch = e2.renderArmPitch.also { e2.renderArmPitch = e1.renderArmPitch }
        e1.prevRenderArmYaw = e2.prevRenderArmYaw.also { e2.prevRenderArmYaw = e1.prevRenderArmYaw }
        e1.prevRenderArmPitch = e2.prevRenderArmPitch.also { e2.prevRenderArmPitch = e1.prevRenderArmPitch }
    }
}

// getInstance actually modifies the instance, so we have to get our reference once and hold on to it
val clippingHelper: ClippingHelper = ClippingHelperImpl.getInstance()

fun glMask(r: Boolean, g: Boolean, b: Boolean, a: Boolean, depth: Boolean, stencil: Int) {
    GlStateManager.colorMask(r, g, b, a)
    GlStateManager.depthMask(depth)
    GL11.glStencilMask(stencil)
}

fun renderFullScreen() {
    val tessellator = Tessellator.getInstance()
    with(tessellator.buffer) {
        setTranslation(0.0, Minecraft.getMinecraft().player.getEyeHeight().toDouble(), 0.0)
        // Drawing a triangular pyramid-ish, because it's the easiest shape to draw which can enclose the camera
        begin(GL11.GL_TRIANGLE_STRIP, DefaultVertexFormats.POSITION)
        // Bottom triangle
        pos(-1.0, -0.5, -1.0).endVertex()
        pos(1.0, -0.5, -1.0).endVertex()
        pos(0.0, -0.5, 1.0).endVertex()
        // Remaining triangles
        pos(0.0, 0.5, 0.0).endVertex()
        pos(-1.0, -0.5, -1.0).endVertex()
        pos(1.0, -0.5, -1.0).endVertex()

        setTranslation(0.0, 0.0, 0.0)
    }
    GlStateManager.disableCull()
    tessellator.draw()
    GlStateManager.enableCull()
}

fun glClipPlane(plane: Int, normal: Vec3d, pointOnPlane: Vec3d) {
    // Plane is where: ax + by + cz + d = 0
    val a = normal.x
    val b = normal.y
    val c = normal.z
    val d = -a * pointOnPlane.x - b * pointOnPlane.y - c * pointOnPlane.z
    val buf = ByteBuffer.allocateDirect(4 * 8)
            .order(ByteOrder.nativeOrder())
            .asDoubleBuffer().put(a).put(b).put(c).put(d)
    buf.flip()
    GL11.glClipPlane(plane, buf)
}
