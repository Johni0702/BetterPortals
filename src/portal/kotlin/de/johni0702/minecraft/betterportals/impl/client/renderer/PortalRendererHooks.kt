package de.johni0702.minecraft.betterportals.impl.client.renderer

import de.johni0702.minecraft.betterportals.client.render.portalDetail
import de.johni0702.minecraft.betterportals.common.*
import de.johni0702.minecraft.betterportals.impl.client.glClipPlane
import de.johni0702.minecraft.view.client.render.renderPassManager
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher.*
import net.minecraft.entity.Entity
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.Vec3d
import org.lwjgl.opengl.GL11
import kotlin.math.sign

internal object PortalRendererHooks {
    private val clippingStack = mutableListOf<Boolean>()
    private val disableParentClippingStack = mutableListOf<Boolean>()
    private val currentRenderPass get() = Minecraft.getMinecraft().renderPassManager.current

    fun beforeRender(entity: Entity): Boolean {
        if (entity is Portal) return true
        if (!entity.isAddedToWorld) return true // e.g. mobs rendered as part of tile entities (e.g. spawner)
        val lowestEntity = entity.lowestRidingEntity
        val entityAABB = generateSequence(entity) { it.ridingEntity }
                .map { it.renderBoundingBox }
                .reduce(AxisAlignedBB::union)
        val entityPos = lowestEntity.syncPos + lowestEntity.eyeOffset

        val (portal, disableParentClipping) = currentRenderPass?.let { instance ->
            // If we're not rendering our own world (i.e. if we're looking through a portal)
            // then we do not want to render entities on the wrong remote side of said portal
            // Or more precisely: we want to clip those outside of the portal and skip those inside/directly behind it.
            val portal = instance.portalDetail?.parent ?: return@let null
            val portalPos = portal.remotePosition.to3dMid()
            val portalFacing = portal.remoteFacing
            val cameraFacing = portalFacing.axis.toFacing(instance.camera.viewPosition - portalPos)
            val entityFacing = portalFacing.axis.toFacing(entityPos - portalPos)
            // The AABB of some entities is too small. Growing it on the portal axis will solve that and should be OK.
            val largeEntityAABB = entityAABB.grow(portalFacing.directionVec.to3d().abs() * 100)
            return@let if (portal.remoteDetailedBounds.any { it.intersects(largeEntityAABB) }) {
                // Inside the portal
                if (cameraFacing == entityFacing) {
                    return false // on the side which we cannot see, skip entirely
                } else {
                    Pair(portal, true) // on the visible side, disable parent clipping so we don't slice it open
                }
            } else {
                // Outside the portal, clip it with the parent clipping plane just like everything else
                Pair(portal, false)
            }
        } ?: Pair(null, false)

        if (disableParentClipping) {
            GL11.glDisable(GL11.GL_CLIP_PLANE5)
        }
        disableParentClippingStack.add(disableParentClipping)

        // We also do not want to render the entity if it's on the opposite side of whatever portal we
        // might be looking at right now (i.e. on the other side of any portals in our world)
        // Actually, we do still want to render it outside the portal frame but only on the right side,
        // because there it'll be visible when looking at the portal from the side.
        val inPortals = entity.world.portalManager.loadedPortals.filter {
            // The AABB of some entities is too small. Growing it on the portal axis will solve that and should be OK.
            val largeEntityAABB = entityAABB.grow(it.portal.localFacing.directionVec.to3d().abs() * 100)
            // ignore the remote end of our current portal
            portal?.isTarget(it.portal) != true
                    // ignore invisible one-way portals
                    && (it !is PortalAgent.OneWay || !it.isTailEnd || it.isTailEndVisible)
                    // the entity has to be even remotely close to it
                    && it.portal.localBoundingBox.intersects(largeEntityAABB)
                    // if it is, then check if it's actually in one of the blocks (and not some hole)
                    && it.portal.localDetailedBounds.any { aabb -> aabb.intersects(largeEntityAABB) }
        }
        // FIXME can't deal with entities which are in more than one portal at the same time
        inPortals.firstOrNull()?.let { agent ->
            val it = agent.portal
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

        return true
    }

    fun afterRender(entity: Entity) {
        if (entity is Portal) return
        if (!entity.isAddedToWorld) return // e.g. mobs rendered as part of tile entities (e.g. spawner)
        if (disableParentClippingStack.removeAt(disableParentClippingStack.size - 1)) {
            GL11.glEnable(GL11.GL_CLIP_PLANE5)
        }
        if (clippingStack.removeAt(clippingStack.size - 1)) {
            GL11.glDisable(GL11.GL_CLIP_PLANE4)
        }
    }
}
