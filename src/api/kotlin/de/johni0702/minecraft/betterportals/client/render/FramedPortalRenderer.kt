package de.johni0702.minecraft.betterportals.client.render

import de.johni0702.minecraft.betterportals.common.*
import de.johni0702.minecraft.view.client.render.RenderPass
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.BufferBuilder
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.RenderHelper
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.client.renderer.texture.TextureMap
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import org.lwjgl.opengl.GL11

open class FramedPortalRenderer<in P: FinitePortal>(
        val textureOpacity: () -> Double = { 0.0 },
        private val portalSprite: () -> TextureAtlasSprite? = { null }
) : PortalRenderer<P>() {
    override fun renderPortalSurface(portal: P, pos: Vec3d, renderPass: RenderPass, haveContent: Boolean) {
        val offset = pos - Vec3d(0.5, 0.5, 0.5)

        val tessellator = Tessellator.getInstance()
        with(tessellator.buffer) {
            begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION)

            val blocks = portal.relativeBlocks.map { it.rotate(portal.localRotation) }
            blocks.forEach { pos ->
                setTranslation(offset.x + pos.x, offset.y + pos.y, offset.z + pos.z)
                if (haveContent) {
                    // When we have remote content for the portal, then render as much as possible as to minimize
                    // entity rendering artifacts inside the portal in the remote world (since they're only rendered
                    // where we draw the portal surface).
                    EnumFacing.VALUES.forEach facing@{ facing ->
                        if (blocks.contains(pos.offset(facing))) return@facing
                        if (facing == viewFacing) return@facing

                        renderPartialPortalFace(this, facing)
                    }
                } else {
                    // otherwise preserve as much of the local frame as feasible and only render black on the far side
                    // to prevent the player from seeing that side.
                    renderPartialPortalFace(this, viewFacing.opposite)
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

    override fun doRenderTransparent(portal: P, pos: Vec3d, partialTicks: Float) {
        super.doRenderTransparent(portal, pos, partialTicks)
        val opacity = textureOpacity()
        if (opacity > 0) {
            renderPortalBlocks(portal, pos, opacity)
        }
    }

    protected open fun renderPortalBlocks(portal: P, pos: Vec3d, opacity: Double) {
        val offset = pos - Vec3d(0.5, 0.5, 0.5)

        val tessellator = Tessellator.getInstance()
        with(tessellator.buffer) {
            begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK)

            val blocks = portal.relativeBlocks.map { it.rotate(portal.localRotation) }
            blocks.forEach { pos ->
                setTranslation(offset.x + pos.x, offset.y + pos.y, offset.z + pos.z)
                renderPortalBlock(portal, pos, opacity, this)
            }

            setTranslation(0.0, 0.0, 0.0)
        }

        val mc = Minecraft.getMinecraft()
        GlStateManager.color(1f, 1f, 1f)
        mc.renderEngine.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE)
        RenderHelper.disableStandardItemLighting()
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
        GlStateManager.enableBlend()
        tessellator.draw()
        GlStateManager.disableBlend()
        RenderHelper.enableStandardItemLighting()
    }

    protected open fun renderPortalBlock(portal: P, relativePos: BlockPos, opacity: Double, bufferBuilder: BufferBuilder) {
        val sprite = portalSprite() ?: return
        bufferBuilder.markSpriteAsActive(sprite)
        val facing = viewFacing.opposite
        var rotFacing = if (facing.axis == EnumFacing.Axis.Y) EnumFacing.NORTH else EnumFacing.UP
        (0..3).map { i ->
            val nextRotFacing = rotFacing.rotateAround(facing.axis).let {
                if (facing.axisDirection == EnumFacing.AxisDirection.POSITIVE) it else it.opposite
            }
            with(bufferBuilder) {
                with(rotFacing.directionVec.to3d() * 0.5 + nextRotFacing.directionVec.to3d() * 0.5 + Vec3d(0.5, 0.5, 0.5)) {
                    pos(x, y, z)
                }
                color(1f, 1f, 1f, opacity.toFloat())
                when (i) {
                    0 -> tex(sprite.minU.toDouble(), sprite.minV.toDouble())
                    1 -> tex(sprite.minU.toDouble(), sprite.maxV.toDouble())
                    2 -> tex(sprite.maxU.toDouble(), sprite.maxV.toDouble())
                    3 -> tex(sprite.maxU.toDouble(), sprite.minV.toDouble())
                }
                lightmap(240, 240)
                endVertex()
            }
            rotFacing = nextRotFacing
        }
    }
}

private fun BufferBuilder.markSpriteAsActive(sprite: TextureAtlasSprite) {
    // See https://github.com/sp614x/optifine/issues/2633
    methodSetSprite?.invoke(this, sprite)
}
private val methodSetSprite by lazy {
    try {
        BufferBuilder::class.java.getDeclaredMethod("setSprite", TextureAtlasSprite::class.java)
    } catch (e: NoSuchMethodException) {
        null
    }
}