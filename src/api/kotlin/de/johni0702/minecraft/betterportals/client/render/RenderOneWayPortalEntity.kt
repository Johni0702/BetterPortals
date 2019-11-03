package de.johni0702.minecraft.betterportals.client.render

import de.johni0702.minecraft.betterportals.common.entity.PortalEntity
import net.minecraft.client.renderer.entity.RenderManager
import net.minecraft.entity.Entity

open class RenderOneWayPortalEntity<E, out R: OneWayFramedPortalRenderer> (
        renderManager: RenderManager,
        portalRenderer: R
) : RenderPortalEntity<E, R>(renderManager, portalRenderer)
        where E: PortalEntity.OneWay,
              E: Entity
{
    override fun doRender(entity: E, x: Double, y: Double, z: Double, entityYaw: Float, partialTicks: Float) {
        portalRenderer.isTailEnd = entity.isTailEnd
        portalRenderer.isTailEndVisible = entity.isTailEndVisible
        // FIXME preprocessor doesn't currently remap this one for yet-to-be-determined reason
        //#if FABRIC>=1
        //$$ super.render(entity, x, y, z, entityYaw, partialTicks)
        //#else
        super.doRender(entity, x, y, z, entityYaw, partialTicks)
        //#endif
    }

    override fun renderMultipass(entity: E, x: Double, y: Double, z: Double, entityYaw: Float, partialTicks: Float) {
        portalRenderer.isTailEnd = entity.isTailEnd
        portalRenderer.isTailEndVisible = entity.isTailEndVisible
        // FIXME preprocessor doesn't currently remap this one for yet-to-be-determined reason
        //#if FABRIC>=1
        //$$ super.renderSecondPass(entity, x, y, z, entityYaw, partialTicks)
        //#else
        super.renderMultipass(entity, x, y, z, entityYaw, partialTicks)
        //#endif
    }
}