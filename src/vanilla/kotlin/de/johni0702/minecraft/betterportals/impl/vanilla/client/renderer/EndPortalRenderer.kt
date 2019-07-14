package de.johni0702.minecraft.betterportals.impl.vanilla.client.renderer

import de.johni0702.minecraft.betterportals.client.render.OneWayFramedPortalRenderer
import de.johni0702.minecraft.betterportals.common.FinitePortal
import de.johni0702.minecraft.betterportals.common.minus
import de.johni0702.minecraft.betterportals.common.plus
import de.johni0702.minecraft.betterportals.common.to3d
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.tileentity.TileEntityEndPortalRenderer
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher
import net.minecraft.tileentity.TileEntityEndPortal
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.Vec3d
import org.lwjgl.opengl.GL14

class EndPortalRenderer<in P: FinitePortal>(
        textureOpacity: () -> Double = { 0.0 }
) : OneWayFramedPortalRenderer<P>(textureOpacity) {
    private val tileEntityRenderer = TileEntityEndPortalRenderer().also {
        it.setRendererDispatcher(TileEntityRendererDispatcher.instance)
    }
    private var firstPass = false
    private var opacity = 0.0
    private val dummyTileEntity = object : TileEntityEndPortal() {
        override fun shouldRenderFace(face: EnumFacing): Boolean {
            if (firstPass) {
                GlStateManager.blendFunc(GlStateManager.SourceFactor.CONSTANT_ALPHA, GlStateManager.DestFactor.ONE_MINUS_CONSTANT_ALPHA)
                GL14.glBlendColor(0f, 0f, 0f, opacity.toFloat())
                firstPass = false
            }
            return when (face) {
                EnumFacing.UP, EnumFacing.DOWN -> true
                else -> false
            }
        }
    }

    private var partialTicks = 0f

    override fun doRenderTransparent(portal: P, pos: Vec3d, partialTicks: Float) {
        this.partialTicks = partialTicks
        super.doRenderTransparent(portal, pos, partialTicks)
    }

    override fun renderPortalBlocks(portal: P, pos: Vec3d, opacity: Double) {
        this.opacity = opacity
        val offset = pos - Vec3d(0.5, 0.5, 0.5)

        val blocks = portal.relativeBlocks.map { it.rotate(portal.localRotation) }
        blocks.forEach { relativePos ->
            with(offset + relativePos.to3d()) {
                firstPass = true
                tileEntityRenderer.render(dummyTileEntity, x, y, z, partialTicks, 0, 1f)
            }
        }
    }
}