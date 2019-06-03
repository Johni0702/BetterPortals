package de.johni0702.minecraft.betterportals.client.render

import de.johni0702.minecraft.betterportals.common.FinitePortal
import de.johni0702.minecraft.betterportals.common.entity.PortalEntity
import net.minecraft.client.renderer.BufferBuilder
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.Vec3d

open class OneWayFramedPortalRenderer<in P: FinitePortal> : FramedPortalRenderer<P>() {
    /**
     * Whether to render the tail/exit end of a pair of portals.
     * See [PortalEntity.OneWay.isTailEnd].
     */
    var isTailEnd = false

    /**
     * Whether the tail end of the pair of portals is currently visible. Ignored if [isTailEnd] is false.
     *
     * The tail end of a one-way portal pair will usually disappear shortly after you've used it.
     */
    var isTailEndVisible = false

    open fun shouldFaceBeRendered(facing: EnumFacing): Boolean {
        // There are usually no blocks at the tail end of the portal, so we need to make sure that, when looking at
        // the head, all frame blocks are rendered from the local world. Specifically, we only want to render the
        // far side face when looking from the local world at the portal head.
        if (!isTailEnd) {
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

    override fun render(portal: P, pos: Vec3d, partialTicks: Float) {
        // For the tail end of the portal, skip drawing unless the player has just passed through it and is still close
        if (isTailEnd && !isTailEndVisible) return
        super.render(portal, pos, partialTicks)
    }
}