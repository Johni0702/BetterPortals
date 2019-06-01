package de.johni0702.minecraft.betterportals.client.renderer

import de.johni0702.minecraft.betterportals.common.entity.AbstractPortalEntity
import de.johni0702.minecraft.view.client.render.RenderPass
import de.johni0702.minecraft.view.client.render.get
import de.johni0702.minecraft.view.client.render.set

class PortalDetail(
        val parent: AbstractPortalEntity
)

var RenderPass.portalDetail: PortalDetail?
    get() = get()
    set(value) { set(value) }