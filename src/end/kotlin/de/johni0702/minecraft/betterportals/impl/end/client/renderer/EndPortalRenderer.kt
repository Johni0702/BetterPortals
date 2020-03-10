package de.johni0702.minecraft.betterportals.impl.end.client.renderer

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

class EndPortalRenderer(
        textureOpacity: () -> Double = { 0.0 }
) : OneWayFramedPortalRenderer(textureOpacity) {
    private val tileEntityRenderer = TileEntityEndPortalRenderer().also {
        // FIXME seems like the perprocessor should be able to handle this? (it does handle the argument, just not the method)
        //#if FABRIC>=1
        //$$ it.setRenderManager(BlockEntityRenderDispatcher.INSTANCE)
        //#else
        it.setRendererDispatcher(TileEntityRendererDispatcher.instance)
        //#endif
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

    override fun doRenderTransparent(portal: FinitePortal, pos: Vec3d, partialTicks: Float) {
        this.partialTicks = partialTicks
        super.doRenderTransparent(portal, pos, partialTicks)
    }

    override fun renderPortalBlocks(portal: FinitePortal, pos: Vec3d, opacity: Double) {
        this.opacity = opacity
        val offset = pos - Vec3d(0.5, 0.5, 0.5)

        val blocks = portal.blocks.map { it.rotate(portal.localRotation) }
        blocks.forEach { relativePos ->
            with(offset + relativePos.to3d()) {
                firstPass = true
                tileEntityRenderer.render(dummyTileEntity, x, y, z, partialTicks, 0
                        //#if MC<11400
                        , 1f
                        //#endif
                )
            }
        }
    }
}