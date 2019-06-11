package de.johni0702.minecraft.betterportals.impl.client.renderer

import de.johni0702.minecraft.betterportals.client.deriveClientPosRotFrom
import de.johni0702.minecraft.betterportals.client.render.PortalDetail
import de.johni0702.minecraft.betterportals.client.render.portalDetail
import de.johni0702.minecraft.betterportals.common.*
import de.johni0702.minecraft.betterportals.impl.client.PostSetupFogEvent
import de.johni0702.minecraft.betterportals.impl.client.glClipPlane
import de.johni0702.minecraft.betterportals.impl.common.maxRenderRecursionGetter
import de.johni0702.minecraft.view.client.render.DetermineRootPassEvent
import de.johni0702.minecraft.view.client.render.PopulateTreeEvent
import de.johni0702.minecraft.view.client.render.RenderPass
import de.johni0702.minecraft.view.client.render.RenderPassEvent
import net.minecraft.client.Minecraft
import net.minecraft.client.entity.EntityPlayerSP
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.util.math.Vec3d
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import org.lwjgl.opengl.GL11

internal object PortalRenderManager {
    var registered by MinecraftForge.EVENT_BUS

    @SubscribeEvent
    fun determineRootPass(event: DetermineRootPassEvent) {
        // Determining the world of the camera is kind of tricky.
        // The syncPos (server-position) of the vehicle which our view entity is riding in directly determines the world.
        // So to determine the view at the camera, we need to trace backwards following any portals we encounter:
        //   - Starting at the server-position of the vehicle (vehicleSyncPos)
        //   - to the current client-side position of the vehicle (vehicleClientPos)
        //      (only EntityMinecart and EntityOtherPlayerMP require this extra step, see Entity#syncPos)
        //   - then to the frame-interpolated position of the vehicle (vehicleInterpPos)
        //   - then up the entity stack to the frame-interpolated position of the view entity (camera.eyePosition)
        //   - finally to the (potentially third-person mode) camera position (camera.viewPosition)
        val mc = Minecraft.getMinecraft()
        var view = event.view
        var camera = event.camera
        var viewEntity = mc.renderViewEntity ?: mc.player
        val vehicle = viewEntity.lowestRidingEntity
        val vehicleSyncPos = vehicle.syncPos + vehicle.eyeOffset
        val vehicleClientPos = vehicle.pos + vehicle.eyeOffset
        // Note: must not use prevPos (or by extension getPositionEyes) since EntityMinecraft completely breaks it
        var vehicleInterpPos = vehicle.lastTickPos + (vehicle.pos - vehicle.lastTickPos) * event.partialTicks.toDouble()
        var pos = vehicleSyncPos
        var target = vehicleClientPos
        var hitClientPosition = if (vehicleSyncPos == vehicleClientPos) true.also { target = vehicleInterpPos } else false
        var hitVisualPosition = false
        var hitPassenger = false
        while (true) {
            val hitInfo = view.camera.world.portalManager.loadedPortals.flatMap { agent ->
                val portal = agent.portal

                // Ignore portals which haven't yet been loaded
                agent.view ?: return@flatMap listOf<Pair<Pair<PortalAgent<*, *>, Vec3d>, Double>>()

                // FIXME handle one-way portals

                // For each portal, find the point intercepting the line between entity and camera
                val vec = portal.localFacing.directionVec.to3d() * 0.5 // FIXME assumes portals to be one block deep
                val negVec = vec * -1
                portal.localDetailedBounds.map {
                    // contract BB to only detect changes crossing 0.5 on the portal axis instead of hitting anywhere in the block
                    val trace = it.contract(vec).contract(negVec).calculatePlaneIntercept(pos, target)
                    Pair(agent, trace)
                }.mapNotNull { (portal, hitVec) ->
                    // and calculate its distance to the entity
                    hitVec ?: return@mapNotNull null
                    Pair(Pair(portal, hitVec), (hitVec - pos).lengthSquared())
                }
            }.minBy {
                // then get the one which is closest to the entity
                it.second
            }?.first

            if (hitInfo != null) {
                val (agent, hitVec) = hitInfo
                val portal = agent.portal

                // If we hit a portal, switch to its view and transform the camera/entity positions accordingly
                // also change the current position to be in the portal so we don't accidentally match any portals
                // behind the one we're looking through.
                view = agent.view!!
                target = (portal.localToRemoteMatrix * target.toPoint()).toMC()
                vehicleInterpPos = (portal.localToRemoteMatrix * vehicleInterpPos.toPoint()).toMC()
                camera = camera.transformed(portal.localToRemoteMatrix)
                pos = (portal.localToRemoteMatrix * hitVec.toPoint()).toMC()
                val prevViewEntity = viewEntity
                viewEntity = agent.view!!.camera
                if (prevViewEntity is EntityPlayerSP) {
                    viewEntity.deriveClientPosRotFrom(prevViewEntity, portal)
                } else {
                    viewEntity.derivePosRotFrom(prevViewEntity, portal)
                }
            } else if (!hitClientPosition) {
                hitClientPosition = true
                pos = target
                target = vehicleInterpPos
            } else if (!hitVisualPosition) {
                hitVisualPosition = true
                pos = target
                target = camera.eyePosition
                if (viewEntity == vehicle) {
                    // Skip right to next step if we aren't in any vehicle
                    hitPassenger = true
                    target = camera.viewPosition
                }
            } else if (!hitPassenger) {
                hitPassenger = true
                pos = target
                target = camera.viewPosition
            } else {
                break
            }
        }

        event.camera = camera
        event.view = view
    }

    @SubscribeEvent
    fun buildPortalTree(event: PopulateTreeEvent) {
        val maxRecursions = maxRenderRecursionGetter()
        val root = event.root
        if (root.addPortals(maxRecursions, root.manager.previous)) {
            event.changed = true
        }
    }

    private fun RenderPass.addPortals(maxRecursions: Int, previousFrame: RenderPass?): Boolean {
        if (maxRecursions <= 0) return false

        with(camera.feetPosition) { camera.frustum.setPosition(x, y, z) }

        var changed = false
        val parentPortal = portalDetail?.parent
        view.camera.world.portalManager.loadedPortals.forEach {
            val portal = it.portal

            // check if there's already a pass for this portal
            if (children.any { it.portalDetail?.parent == portal }) return@forEach
            // portal must be visible (i.e. must not be frustum culled)
            if (!it.canBeSeen(camera.frustum)) return@forEach
            // its view must have been loaded (otherwise there's nothing to render)
            it.view ?: return@forEach
            // it must not be our parent (the portal from which this world is being viewed)
            if (parentPortal?.isTarget(portal) == true) return@forEach

            val childPreviousFrame = previousFrame?.children?.find { it.portalDetail?.parent == portal }

            val childCamera = camera.transformed(portal.localToRemoteMatrix).let {
                it.withFrustum(PortalCamera(portal, it.viewPosition, it.frustum))
            }
            val plan = addChild(it.view!!, childCamera, childPreviousFrame)
            val cameraSide = portal.localFacing.axis.toFacing(camera.viewPosition - portal.localPosition.to3dMid())
            plan.portalDetail = PortalDetail(portal, cameraSide)
            changed = true
        }
        children.forEach { child ->
            val portal = child.portalDetail?.parent ?: return@forEach
            val childPreviousFrame = previousFrame?.children?.find { it.portalDetail?.parent == portal }
            if (child.addPortals(maxRecursions - 1, childPreviousFrame)) {
                changed = true
            }
        }
        return changed
    }

    @SubscribeEvent
    fun startRenderPass(event: RenderPassEvent.Start) {
        val mc = Minecraft.getMinecraft()
        val partialTicks = event.partialTicks
        val renderPass = event.renderPass
        val camera = renderPass.camera
        val parentPortal = renderPass.portalDetail?.parent
        if (parentPortal != null) {
            // Setup clipping plane for parent portal
            // The render is supposed to look like this from the parent (we are currently rendering Remote World):
            // Camera -> Local World -> Portal -> Remote World
            // However, we are actually rendering:
            // Camera -> Remote World -> Portal -> Remote World
            // so we need to clip away the remote world on the camera side of the portal, otherwise blocks in the
            // Remote World which are between the camera and the portal will show up in the final composed render.
            val portalPos = parentPortal.remotePosition.to3dMid()
            val cameraSide = parentPortal.remoteAxis.toFacing(camera.viewPosition - portalPos)
            // Position clipping plane on the camera side of the portal such that the portal frame is fully rendered
            val planePos = parentPortal.remotePosition.to3dMid() + cameraSide.directionVec.to3d().scale(0.5)
            // glClipPlane uses the current ModelView matrix to transform the given coordinates to view space
            // so we need to have the camera setup before calling it
            mc.entityRenderer.setupCameraTransform(partialTicks, 0)
            // setupCameraTransform configures world space with the origin at the camera's feet.
            // planePos however is currently absolute world space, so we need to convert it
            val relPlanePos = planePos - camera.feetPosition
            glClipPlane(GL11.GL_CLIP_PLANE5, cameraSide.directionVec.to3d().scale(-1.0), relPlanePos)
            GL11.glEnable(GL11.GL_CLIP_PLANE5) // FIXME don't hard-code clipping plane id

            // Reduce fog by distance between camera and portal, we will later re-apply this distance worth of fog
            // to the rendered portal but then with the fog of the correct dimension.
            // This won't give quite correct results for large portals but far better ones than using the incorrect fog.
            val dist = (camera.viewPosition - portalPos).lengthVector().toFloat()
            when (GlStateManager.FogMode.values().find { it.capabilityId == GlStateManager.fogState.mode }) {
                GlStateManager.FogMode.LINEAR -> fogOffset = dist
                // TODO
                else -> fogOffset = 0f
            }
        }
    }

    @SubscribeEvent
    fun endRenderPass(event: RenderPassEvent.End) {
        fogOffset = 0f
        GL11.glDisable(GL11.GL_CLIP_PLANE5) // FIXME don't hard-code clipping plane id
    }

    private var fogOffset = 0.toFloat()

    @SubscribeEvent(priority = EventPriority.LOWEST)
    fun postSetupFog(event: PostSetupFogEvent) {
        if (fogOffset != 0f) {
            GlStateManager.setFogStart(GlStateManager.fogState.start + fogOffset)
            GlStateManager.setFogEnd(GlStateManager.fogState.end + fogOffset)
        }
    }
}