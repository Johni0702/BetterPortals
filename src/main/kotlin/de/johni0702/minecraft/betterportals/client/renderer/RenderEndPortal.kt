package de.johni0702.minecraft.betterportals.client.renderer

import de.johni0702.minecraft.betterportals.common.entity.EndPortalEntity
import net.minecraft.client.renderer.BufferBuilder
import net.minecraft.client.renderer.entity.RenderManager
import net.minecraft.util.EnumFacing

class RenderEndPortal(renderManager: RenderManager) : RenderOneWayPortal<EndPortalEntity>(renderManager) {
    override fun createInstance(entity: EndPortalEntity, x: Double, y: Double, z: Double, partialTicks: Float): Instance =
            Instance(entity, x, y, z, partialTicks)

    class Instance(entity: EndPortalEntity, x: Double, y: Double, z: Double, partialTicks: Float)
        : RenderOneWayPortal.Instance<EndPortalEntity>(entity, x, y, z, partialTicks) {
        override fun shouldFaceBeRendered(facing: EnumFacing): Boolean {
            // The end portal frame aren't full blocks, so when viewing from the bottom and when only the top face is
            // drawn (as usually is for one-way-portals), you can look behind the portal between its face and frame.
            // To prevent that, when looking from the bottom, the sideways faces are drawn as well.
            if (!entity.isTailEnd) {
                if (viewFacing == EnumFacing.DOWN && facing != EnumFacing.DOWN) return true
            }
            return super.shouldFaceBeRendered(facing)
        }
    }
}