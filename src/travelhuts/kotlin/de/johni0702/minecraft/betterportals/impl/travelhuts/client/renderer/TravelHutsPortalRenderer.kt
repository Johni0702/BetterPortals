//#if MC<11400
package de.johni0702.minecraft.betterportals.impl.travelhuts.client.renderer

import de.johni0702.minecraft.betterportals.client.render.FramedPortalRenderer
import de.johni0702.minecraft.betterportals.common.*
import de.johni0702.minecraft.betterportals.impl.travelhuts.common.TRAVELHUTS_MOD_ID
import de.johni0702.minecraft.view.client.render.RenderPass
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.Vec3d
import org.lwjgl.opengl.GL11

class TravelHutsPortalRenderer(
        textureOpacity: () -> Double
) : FramedPortalRenderer(
        textureOpacity,
        { Minecraft.getMinecraft().textureMapBlocks.getAtlasSprite("$TRAVELHUTS_MOD_ID:blocks/portal") }
) {
    override fun renderPortalSurface(portal: FinitePortal, pos: Vec3d, renderPass: RenderPass, haveContent: Boolean) {
        val offset = pos - Vec3d(0.5, 0.5, 0.5) - portal.localFacing.rotateY().directionVec.to3d().abs()

        val tessellator = Tessellator.getInstance()
        with(tessellator.buffer) {
            begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION)

            setTranslation(offset.x, offset.y, offset.z)

            val d0 =  1 / 16.0
            val d1 = 15 / 16.0
            val d2 = 15 / 16.0 + 1
            val d3 = 15 / 16.0 + 2
            val back = if (viewFacing.axisDirection == EnumFacing.AxisDirection.POSITIVE) d0 else d1

            if (portal.localFacing.axis == EnumFacing.Axis.X) {
                // Bottom
                pos(d0, d0, d0).endVertex()
                pos(d1, d0, d0).endVertex()
                pos(d1, d0, d2).endVertex()
                pos(d0, d0, d2).endVertex()

                // Back side
                pos(back, d0, d0).endVertex()
                pos(back, d0, d2).endVertex()
                pos(back, d3, d2).endVertex()
                pos(back, d3, d0).endVertex()

                // Side towards negative Z
                pos(d0, d0, d0).endVertex()
                pos(d1, d0, d0).endVertex()
                pos(d1, d3, d0).endVertex()
                pos(d0, d3, d0).endVertex()

                // Side towards positive Z
                pos(d0, d0, d2).endVertex()
                pos(d1, d0, d2).endVertex()
                pos(d1, d3, d2).endVertex()
                pos(d0, d3, d2).endVertex()
            } else {
                // Bottom
                pos(d0, d0, d0).endVertex()
                pos(d2, d0, d0).endVertex()
                pos(d2, d0, d1).endVertex()
                pos(d0, d0, d1).endVertex()

                // Back side
                pos(d0, d0, back).endVertex()
                pos(d2, d0, back).endVertex()
                pos(d2, d3, back).endVertex()
                pos(d0, d3, back).endVertex()

                // Side towards negative X
                pos(d0, d0, d0).endVertex()
                pos(d0, d0, d1).endVertex()
                pos(d0, d3, d1).endVertex()
                pos(d0, d3, d0).endVertex()

                // Side towards positive X
                pos(d2, d0, d0).endVertex()
                pos(d2, d0, d1).endVertex()
                pos(d2, d3, d1).endVertex()
                pos(d2, d3, d0).endVertex()
            }

            setTranslation(0.0, 0.0, 0.0)
        }

        GlStateManager.disableCull()
        GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL)
        GL11.glPolygonOffset(-1f, -1f)
        tessellator.draw()
        GL11.glPolygonOffset(0f, 0f)
        GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL)
        GlStateManager.enableCull()
    }

    override fun getPortalSpriteUVs(sprite: TextureAtlasSprite): Array<Pair<Double, Double>> = arrayOf(
            Pair((sprite.minU + sprite.maxU).toDouble() / 2, sprite.minV.toDouble()),
            Pair((sprite.minU + sprite.maxU).toDouble() / 2, sprite.maxV.toDouble()),
            Pair(sprite.maxU.toDouble(), sprite.maxV.toDouble()),
            Pair(sprite.maxU.toDouble(), sprite.minV.toDouble())
    )
}
//#endif
