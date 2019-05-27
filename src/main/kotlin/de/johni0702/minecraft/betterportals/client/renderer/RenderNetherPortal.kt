package de.johni0702.minecraft.betterportals.client.renderer

import de.johni0702.minecraft.betterportals.common.entity.NetherPortalEntity
import net.minecraft.client.renderer.entity.RenderManager

class RenderNetherPortal(renderManager: RenderManager) : AbstractRenderPortal<NetherPortalEntity>(renderManager) {
    override fun createInstance(entity: NetherPortalEntity, x: Double, y: Double, z: Double, partialTicks: Float): Instance =
            Instance(entity, x, y, z, partialTicks)

    class Instance(entity: NetherPortalEntity, x: Double, y: Double, z: Double, partialTicks: Float)
        : AbstractRenderPortal.Instance<NetherPortalEntity>(entity, x, y, z, partialTicks)
}