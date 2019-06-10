package de.johni0702.minecraft.betterportals.impl.client.renderer

import de.johni0702.minecraft.betterportals.client.render.portalDetail
import de.johni0702.minecraft.betterportals.common.*
import de.johni0702.minecraft.betterportals.impl.client.glClipPlane
import de.johni0702.minecraft.view.client.render.renderPassManager
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher.*
import net.minecraft.entity.Entity
import net.minecraft.util.math.Vec3d
import org.lwjgl.opengl.GL11
import kotlin.math.sign

internal object PortalRendererHooks {
    private val clippingStack = mutableListOf<Boolean>()
    private val currentRenderPass get() = Minecraft.getMinecraft().renderPassManager.current

    fun beforeRender(entity: Entity): Boolean {
        if (entity is Portal) return true
        val portal = currentRenderPass?.let { instance ->
            // If we're not rendering our own world (i.e. if we're looking through a portal)
            // then we do not want to render entities on the wrong remote side of said portal
            val portal = instance.portalDetail?.parent ?: return@let null
            val portalPos = portal.remotePosition.to3dMid()
            val facing = portal.remoteFacing.axis.toFacing(instance.camera.viewPosition - portalPos)
            // We need to take the top most y of the entity because otherwise when looking throw a horizontal portal
            // from the below, we might see the head of entities whose feet are below the portal y
            // Same goes the other way around
            val entityBottomPos = entity.syncPos
            val entityTopPos = entityBottomPos + Vec3d(0.0, entity.entityBoundingBox.sizeY, 0.0)
            val relativeBottomPosition = entityBottomPos.subtract(portalPos)
            val relativeTopPosition = entityTopPos.subtract(portalPos)
            if (relativeBottomPosition.dotProduct(facing.directionVec.to3d()) > 0
                    && relativeTopPosition.dotProduct(facing.directionVec.to3d()) > 0) return false
            return@let portal
        }

        // We also do not want to render the entity if it's on the opposite side of whatever portal we
        // might be looking at right now (i.e. on the other side of any portals in our world)
        // Actually, we do still want to render it outside the portal frame but only on the right side,
        // because there it'll be visible when looking at the portal from the side.
        val entityAABB = entity.renderBoundingBox
        val inPortals = entity.world.portalManager.loadedPortals.filter {
            // ignore the remote end of our current portal
            portal?.isTarget(it.portal) != true
                    // the entity has to be even remotely close to it
                    && it.portal.localBoundingBox.intersects(entityAABB)
                    // if it is, then check if it's actually in one of the blocks (and not some hole)
                    && it.portal.localDetailedBounds.any { aabb -> aabb.intersects(entityAABB) }
        }
        // FIXME can't deal with entities which are in more than one portal at the same time
        inPortals.firstOrNull()?.let { agent ->
            val it = agent.portal
            val entityPos = entity.syncPos + entity.eyeOffset
            val portalPos = it.localPosition.to3dMid()
            val relEntityPos = entityPos - it.localPosition.to3dMid()
            val portalFacing = it.localFacing
            val portalDir = portalFacing.directionVec.to3d()
            val planeDir = portalDir.scale(sign(relEntityPos.dotProduct(portalDir)))
            val playerPos = Vec3d(staticPlayerX, staticPlayerY, staticPlayerZ)
            val relPortalPos = portalPos - playerPos
            // Note: playerPos will break in 3rd person but we only use it as fallback
            val cameraPos = currentRenderPass?.camera?.viewPosition ?: playerPos
            val cameraSide = portalFacing.axis.toFacing(cameraPos - portalPos)
            val planeOffset = cameraSide.directionVec.to3d().scale(-0.5)
            val planePos = relPortalPos + planeOffset
            glClipPlane(GL11.GL_CLIP_PLANE4, planeDir, planePos)
            GL11.glEnable(GL11.GL_CLIP_PLANE4) // FIXME don't hard-code clipping plane id
        }
        clippingStack.add(inPortals.isNotEmpty())

        GL11.glDisable(GL11.GL_CLIP_PLANE5)
        return true
    }

    fun afterRender(entity: Entity) {
        if (entity is Portal) return
        if (clippingStack.removeAt(clippingStack.size - 1)) {
            GL11.glDisable(GL11.GL_CLIP_PLANE4)
        }

        if (currentRenderPass?.portalDetail?.parent == null) return

        GL11.glEnable(GL11.GL_CLIP_PLANE5)
    }
}
