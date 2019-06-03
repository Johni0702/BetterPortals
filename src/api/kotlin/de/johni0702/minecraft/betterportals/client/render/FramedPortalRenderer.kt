package de.johni0702.minecraft.betterportals.client.render

import de.johni0702.minecraft.betterportals.common.*
import de.johni0702.minecraft.view.client.render.RenderPass
import net.minecraft.client.renderer.BufferBuilder
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.Vec3d
import org.lwjgl.opengl.GL11

open class FramedPortalRenderer<in P: FinitePortal> : PortalRenderer<P>() {
    override fun renderPortalSurface(portal: P, pos: Vec3d, renderPass: RenderPass) {
        val offset = pos - Vec3d(0.5, 0.5, 0.5)

        val tessellator = Tessellator.getInstance()
        with(tessellator.buffer) {
            begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION)

            val blocks = portal.relativeBlocks.map { it.rotate(portal.localRotation) }
            blocks.forEach { pos ->
                setTranslation(offset.x + pos.x, offset.y + pos.y, offset.z + pos.z)
                EnumFacing.VALUES.forEach facing@ { facing ->
                    if (blocks.contains(pos.offset(facing))) return@facing
                    if (facing == viewFacing) return@facing

                    renderPartialPortalFace(this, facing)
                }
            }

            setTranslation(0.0, 0.0, 0.0)
        }

        GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL)
        GL11.glPolygonOffset(-1f, -1f)
        tessellator.draw()
        GL11.glPolygonOffset(0f, 0f)
        GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL)
    }

    protected open fun renderPartialPortalFace(bufferBuilder: BufferBuilder, facing: EnumFacing) {
        // Drawing a cube has never been easier
        val xF = facing.frontOffsetX * 0.5
        val yF = facing.frontOffsetY * 0.5
        val zF = facing.frontOffsetZ * 0.5
        var rotFacing = if (facing.axis == EnumFacing.Axis.Y) EnumFacing.NORTH else EnumFacing.UP
        (0..3).map { _ ->
            val nextRotFacing = rotFacing.rotateAround(facing.axis).let {
                if (facing.axisDirection == EnumFacing.AxisDirection.POSITIVE) it else it.opposite
            }
            bufferBuilder.pos(
                    xF + rotFacing.frontOffsetX * 0.5 + nextRotFacing.frontOffsetX * 0.5 + 0.5,
                    (yF + rotFacing.frontOffsetY * 0.5 + nextRotFacing.frontOffsetY * 0.5 + 0.5),
                    zF + rotFacing.frontOffsetZ * 0.5 + nextRotFacing.frontOffsetZ * 0.5 + 0.5
            ).endVertex()
            rotFacing = nextRotFacing
        }
    }
}