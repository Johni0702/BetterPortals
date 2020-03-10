package de.johni0702.minecraft.betterportals.impl.end.client.tile.renderer

import net.minecraft.client.renderer.tileentity.TileEntityEndPortalRenderer
import net.minecraft.tileentity.TileEntityEndPortal

class BetterEndPortalTileRenderer : TileEntityEndPortalRenderer() {
    override fun render(te: TileEntityEndPortal, x: Double, y: Double, z: Double, partialTicks: Float, destroyStage: Int
                        //#if MC<11400
                        , alpha: Float
                        //#endif
    ) {
    }
}