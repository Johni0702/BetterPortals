package de.johni0702.minecraft.betterportals.client.render

import de.johni0702.minecraft.betterportals.common.FinitePortal
import de.johni0702.minecraft.betterportals.common.entity.PortalEntity
import net.minecraft.client.renderer.entity.RenderManager
import net.minecraft.entity.Entity

open class RenderOneWayPortalEntity<E, P: FinitePortal.Mutable, out R: OneWayFramedPortalRenderer<P>> (
        renderManager: RenderManager,
        portalRenderer: R
) : RenderPortalEntity<E, P, R>(renderManager, portalRenderer)
        where E: PortalEntity.OneWay<P>,
              E: Entity
{
    override fun doRender(entity: E, x: Double, y: Double, z: Double, entityYaw: Float, partialTicks: Float) {
        portalRenderer.isTailEnd = entity.isTailEnd
        portalRenderer.isTailEndVisible = entity.isTailEndVisible
        super.doRender(entity, x, y, z, entityYaw, partialTicks)
    }
}