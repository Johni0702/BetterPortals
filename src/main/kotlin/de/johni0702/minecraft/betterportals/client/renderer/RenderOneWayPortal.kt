package de.johni0702.minecraft.betterportals.client.renderer

import de.johni0702.minecraft.betterportals.common.entity.OneWayPortalEntity
import net.minecraft.client.renderer.BufferBuilder
import net.minecraft.client.renderer.entity.RenderManager
import net.minecraft.util.EnumFacing

open class RenderOneWayPortal<T : OneWayPortalEntity>(renderManager: RenderManager) : AbstractRenderPortal<T>(renderManager) {
    override fun createInstance(entity: T, x: Double, y: Double, z: Double, partialTicks: Float): RenderOneWayPortal.Instance<T> =
            Instance(entity, x, y, z, partialTicks)

    open class Instance<T : OneWayPortalEntity>(entity: T, x: Double, y: Double, z: Double, partialTicks: Float)
        : AbstractRenderPortal.Instance<T>(entity, x, y, z, partialTicks) {

        open fun shouldFaceBeRendered(facing: EnumFacing): Boolean {
            // There are usually no blocks at the tail end of the portal, so we need to make sure that, when looking at
            // the head, all frame blocks are rendered from the local world. Specifically, we only want to render the
            // far side face when looking from the local world at the portal head.
            if (!entity.isTailEnd) {
                if (viewFacing != facing.opposite) {
                    return false
                }
            }
            return true
        }

        override fun renderPartialPortalFace(bufferBuilder: BufferBuilder, facing: EnumFacing) {
            if (!shouldFaceBeRendered(facing)) return
            super.renderPartialPortalFace(bufferBuilder, facing)
        }

        override fun render() {
            // For the tail end of the portal, skip drawing unless the player has just passed through it and is still close
            if (entity.isTailEnd && !entity.isTravelingInProgress) return
            super.render()
        }
    }
}