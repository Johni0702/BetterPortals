package de.johni0702.minecraft.betterportals.client.render

import de.johni0702.minecraft.betterportals.common.Portal
import de.johni0702.minecraft.betterportals.common.entity.PortalEntity
import net.minecraft.client.renderer.entity.Render
import net.minecraft.client.renderer.entity.RenderManager
import net.minecraft.entity.Entity
import net.minecraft.util.ResourceLocation
import net.minecraft.util.math.Vec3d

open class RenderPortalEntity<E, P: Portal.Mutable, out R: PortalRenderer<P>>(
        renderManager: RenderManager,
        val portalRenderer: R
) : Render<E>(renderManager)
        where E: PortalEntity<P>,
              E: Entity
{
    override fun doRender(entity: E, x: Double, y: Double, z: Double, entityYaw: Float, partialTicks: Float) {
        if (entity.isDead) {
            return
        }

        portalRenderer.render(entity.agent.portal, Vec3d(x, y, z), partialTicks)
    }

    override fun doRenderShadowAndFire(entityIn: Entity, x: Double, y: Double, z: Double, yaw: Float, partialTicks: Float) {}

    override fun getEntityTexture(entity: E): ResourceLocation? = null
}