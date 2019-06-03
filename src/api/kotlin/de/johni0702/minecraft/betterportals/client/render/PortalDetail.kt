package de.johni0702.minecraft.betterportals.client.render

import de.johni0702.minecraft.betterportals.common.Portal
import de.johni0702.minecraft.view.client.render.RenderPass
import de.johni0702.minecraft.view.client.render.get
import de.johni0702.minecraft.view.client.render.set
import net.minecraft.util.EnumFacing

class PortalDetail(
        /**
         * The portal through which this view is being rendered.
         * This is not the portal in the current world, it is the one in the world of the parent render pass.
         */
        val parent: Portal,

        /**
         * Side of the parent portal on which the camera is located.
         *
         * Things in the same dimension but on the opposite side of the portal will not be visible.
         */
        val cameraSide: EnumFacing
)

var RenderPass.portalDetail: PortalDetail?
    get() = get()
    set(value) { set(value) }