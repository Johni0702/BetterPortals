package de.johni0702.minecraft.betterportals.client.render

import de.johni0702.minecraft.betterportals.common.Portal
import de.johni0702.minecraft.betterportals.common.PortalAgent
import de.johni0702.minecraft.view.client.render.RenderPass
import de.johni0702.minecraft.view.client.render.get
import de.johni0702.minecraft.view.client.render.set
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.Vec3d

class PortalDetail(
        /**
         * The portal agent through which this view is being rendered.
         * This is not the one in the current world, it is the one in the world of the parent render pass.
         */
        val parentAgent: PortalAgent<*>,

        /**
         * Side of the parent portal on which the camera is located.
         *
         * Things in the same dimension but on the opposite side of the portal will not be visible.
         */
        val cameraSide: EnumFacing
) {
    /**
     * The portal through which this view is being rendered.
     * This is not the portal in the current world, it is the one in the world of the parent render pass.
     */
    val parent: Portal
        get() = parentAgent.portal
}

var RenderPass.portalDetail: PortalDetail?
    get() = get()
    set(value) { set(value) }

/**
 * Portals may fade out based on distance for performance reasons (once it's completely foggy, you no longer need to
 * render it).
 *
 * This fog is independent from the ordinary world fog (though the color will usually be the same).
 */
class PortalFogDetail(
        /**
         * How dense the fog should be.
         * At 0 the view is completely clear.
         * At 1 the remote world is completely obstructed by the fog.
         */
        val density: Double,

        /**
         * Color of the fog (RGB).
         * Will be set depending on the remote dimension.
         */
        var color: Vec3d = Vec3d.ZERO
)

var RenderPass.portalFogDetail: PortalFogDetail?
    get() = get()
    set(value) { set(value) }
