package de.johni0702.minecraft.betterportals.client.renderer

import de.johni0702.minecraft.betterportals.BetterPortalsMod
import de.johni0702.minecraft.betterportals.client.FramebufferD
import de.johni0702.minecraft.betterportals.client.OcclusionQuery
import de.johni0702.minecraft.betterportals.client.PostSetupFogEvent
import de.johni0702.minecraft.betterportals.client.glClipPlane
import de.johni0702.minecraft.betterportals.client.view.ClientView
import de.johni0702.minecraft.betterportals.client.view.ClientViewImpl
import de.johni0702.minecraft.betterportals.common.*
import de.johni0702.minecraft.betterportals.common.entity.AbstractPortalEntity
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.culling.Frustum
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.Vec3d
import net.minecraft.world.World
import net.minecraftforge.client.event.EntityViewRenderEvent
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import org.lwjgl.opengl.GL11
import java.util.*
import javax.vecmath.Point3d

class ViewRenderManager {
    companion object {
        val INSTANCE = ViewRenderManager()
    }
    private var frameWidth = 0
    private var frameHeight = 0
    private val framebufferPool = mutableListOf<FramebufferD>()
    private val eventHandler = EventHandler()
    init {
        eventHandler.registered = true
    }
    private val occlusionQueries = mutableMapOf<AbstractPortalEntity, OcclusionQuery>()
    private val disposedOcclusionQueries = mutableListOf<OcclusionQuery>()
    var fogOffset: Float
        get() = eventHandler.fogOffset
        set(value) {
            eventHandler.fogOffset = value
        }

    fun allocFramebuffer() = framebufferPool.popOrNull() ?: FramebufferD(frameWidth, frameHeight)

    fun releaseFramebuffer(framebuffer: FramebufferD) {
        framebufferPool.add(framebuffer)
    }

    fun getOcclusionQuery(entity: AbstractPortalEntity) = occlusionQueries.getOrPut(entity, ::OcclusionQuery)

    /**
     * Determine the camera's current world, prepare all portals and render the world.
     */
    fun renderWorld(finishTimeNano: Long) {
        val mc = Minecraft.getMinecraft()

        if (mc.displayWidth != frameWidth || mc.displayHeight != frameHeight) {
            frameWidth = mc.displayWidth
            frameHeight = mc.displayHeight
            framebufferPool.forEach { it.deleteFramebuffer() }
            framebufferPool.clear()
        }

        mc.mcProfiler.startSection("determineVisiblePortals")
        val viewEntity = mc.renderViewEntity ?: mc.player
        var view = BetterPortalsMod.viewManager.mainView
        (view as ClientViewImpl).captureState(mc) // capture main view camera
        val viewEntityPos = viewEntity.prevPos + (viewEntity.pos - viewEntity.prevPos) * mc.renderPartialTicks.toDouble()
        var pos = viewEntityPos + Vec3d(0.0, viewEntity.eyeHeight.toDouble(), 0.0)
        // TODO do third person camera
        var cameraPos = pos
        var cameraYaw = viewEntity.prevRotationYaw + (viewEntity.rotationYaw - viewEntity.prevRotationYaw) * mc.renderPartialTicks.toDouble()

        var parentPortal: AbstractPortalEntity? = null

        // Ray trace from the entity (eye) position backwards to the camera position, following any portals which therefore
        // the camera must be looking through.
        while (true) {
            val hitInfo = view.camera.world.getEntities(AbstractPortalEntity::class.java) {
                // FIXME handle one-way portals
                // Ignore portals which haven't yet been loaded or have already been destroyed
                it?.view != null && !it.isDead
                        // or have already been used in the previous iteration
                        && (it.view != parentPortal?.view || it.localPosition != parentPortal?.remotePosition)
            }.flatMap { portal ->
                // For each portal, find the point intercepting the line between entity and camera
                portal.localBlocks.map {
                    // FIXME this should only detect changes from >0.5 to <0.5 in portal direction instead of hitting anywhere in the block
                    val trace = AxisAlignedBB(it).calculateIntercept(pos, cameraPos)
                    Pair(portal, trace)
                }.filter {
                    it.second != null
                }.map { (portal, trace) ->
                    // and calculate its distance to the entity
                    val hitVec = trace!!.hitVec
                    Pair(Pair(portal, hitVec), (hitVec - pos).lengthSquared())
                }
            }.minBy {
                // then get the one which is closest to the entity
                it.second
            }?.first

            if (hitInfo != null) {
                val (portal, hitVec) = hitInfo

                // If we hit a portal, switch to its view and transform the camera position accordingly
                // also change the entity position to be in the portal so we don't accidentally match any portals
                // behind the one we're looking through.
                view = portal.view!!
                cameraPos = (portal.localToRemoteMatrix * cameraPos.toPoint()).toMC()
                cameraYaw += (portal.remoteRotation - portal.localRotation).degrees.toDouble()
                pos = (portal.localToRemoteMatrix * hitVec.toPoint()).toMC()
                parentPortal = portal
            } else {
                break
            }
        }

        mc.mcProfiler.endSection()

        // Capture camera properties (rotation, fov)
        GlStateManager.pushAttrib()
        GlStateManager.pushMatrix()
        eventHandler.capture = true
        eventHandler.mainCameraYaw = cameraYaw.toFloat()
        val camera = view.withView {
            mc.entityRenderer.setupCameraTransform(mc.renderPartialTicks, 0)
            val entity = mc.renderViewEntity!!
            val entityPos = entity.lastTickPos + (entity.pos - entity.lastTickPos) * mc.renderPartialTicks.toDouble()
            Frustum().apply { setPosition(entityPos.x, entityPos.y, entityPos.z) }
        }
        eventHandler.capture = false
        GlStateManager.popMatrix()
        GlStateManager.popAttrib()

        // Build render plan
        val plan = ViewRenderPlan(this, null, view, camera, cameraPos, cameraYaw.toFloat(), 5)

        // Update occlusion queries
        occlusionQueries.values.forEach { it.update() }

        // Cleanup occlusion queries for portals which are no longer visible
        val knownPortals = plan.allPortals.toSet()
        occlusionQueries.entries.removeIf { (portal, query) ->
            if (knownPortals.contains(portal)) {
                false
            } else {
                disposedOcclusionQueries.add(query)
                true
            }
        }
        disposedOcclusionQueries.removeIf { it.update() }

        // execute
        mc.framebuffer.unbindFramebuffer()
        val framebuffer = plan.render(finishTimeNano)
        mc.framebuffer.bindFramebuffer(true)

        mc.mcProfiler.startSection("renderFramebuffer")
        framebuffer.framebufferRender(frameWidth, frameHeight)
        mc.mcProfiler.endSection()

        releaseFramebuffer(framebuffer)
    }

    private inner class EventHandler {
        var registered by MinecraftForge.EVENT_BUS

        var capture = false
        var mainCameraYaw = 0.toFloat()
        var fogOffset = 0.toFloat()
        private var yaw = 0.toFloat()
        private var pitch = 0.toFloat()
        private var roll = 0.toFloat()

        @SubscribeEvent(priority = EventPriority.LOWEST)
        fun onCameraSetup(event: EntityViewRenderEvent.CameraSetup) {
            if (capture) {
                yaw = event.yaw
                pitch = event.pitch
                roll = event.roll
            } else {
                val plan = ViewRenderPlan.CURRENT ?: return
                event.yaw = yaw - mainCameraYaw + plan.cameraYaw
                event.pitch = pitch
                event.roll = roll
            }
        }

        private var fov: Float = 0.toFloat()
        @SubscribeEvent(priority = EventPriority.LOWEST)
        fun onFOVSetup(event: EntityViewRenderEvent.FOVModifier) {
            if (capture) {
                fov = event.fov
            } else {
                event.fov = fov
            }
        }

        @SubscribeEvent(priority = EventPriority.LOWEST)
        fun postSetupFog(event: PostSetupFogEvent) {
            if (fogOffset != 0f) {
                GlStateManager.setFogStart(GlStateManager.fogState.start + fogOffset)
                GlStateManager.setFogEnd(GlStateManager.fogState.end + fogOffset)
            }
        }
    }
}

class ViewRenderPlan(
        val manager: ViewRenderManager,
        val parentPortal: AbstractPortalEntity?,
        val view: ClientView,
        val camera: Frustum,
        val cameraPos: Vec3d,
        val cameraYaw: Float,
        val maxRecursions: Int
) {
    companion object {
        var CURRENT: ViewRenderPlan? = null
    }
    val world: World = view.camera.world

    val dependencies = if (maxRecursions > 0)
        world.getEntities(AbstractPortalEntity::class.java) {
            // portal must be visible (i.e. must not be frustum culled)
            it!!.canBeSeen(camera)
                    // its view must have been loaded (otherwise there's nothing to render)
                    && it.view != null
                    // it must not be our parent (the portal from which this world is being viewed)
                    // that is, it must either link to a different world or to a different place than our parent portal
                    && (it.view != parentPortal?.view || it.remotePosition != parentPortal?.localPosition)
                    // it must not be occluded by blocks
                    && !manager.getOcclusionQuery(it).occluded
        }.map { portal ->
            val rotation = portal.remoteRotation - portal.localRotation
            val cameraYaw = this.cameraYaw + rotation.degrees.toFloat()
            val cameraPos = with(this.cameraPos) { portal.localToRemoteMatrix * Point3d(x, y, z) }.toMC()
            val camera = PortalCamera(portal, cameraPos, camera)
            val plan = ViewRenderPlan(manager, portal, portal.view!!, camera, cameraPos, cameraYaw, maxRecursions - 1)
            Pair(portal, plan)
        }
    else
        Collections.emptyList()

    val framebuffers = mutableMapOf<AbstractPortalEntity, FramebufferD>()

    val allPortals: List<AbstractPortalEntity>
        get() = listOfNotNull(parentPortal) + dependencies.flatMap { it.second.allPortals }

    /**
     * Render all dependencies of this view (including transitive ones).
     */
    private fun renderDeps() = dependencies.map { (portal, plan) ->
        portal.onUpdate() // Update position (and other state) of the view entity
        val framebuffer = plan.render(0)
        framebuffers[portal] = framebuffer
        framebuffer
    }

    /**
     * Render this view.
     * Requires all dependencies to have previously been rendered (e.g. by calling [renderDeps]), otherwise their
     * portals will be empty.
     */
    private fun renderSelf(finishTimeNano: Long): FramebufferD {
        if (view.manager.activeView != view) {
            return view.withView { renderSelf(finishTimeNano) }
        }
        val mc = Minecraft.getMinecraft()
        val framebuffer = manager.allocFramebuffer()

        world.profiler.startSection("renderView" + view.id)
        framebuffer.bindFramebuffer(false)
        GlStateManager.pushAttrib()
        GlStateManager.pushMatrix()

        // Clear framebuffer
        GlStateManager.disableFog()
        GlStateManager.disableLighting()
        mc.entityRenderer.disableLightmap()
        mc.entityRenderer.updateFogColor(mc.renderPartialTicks)
        GL11.glClearDepth(1.0)
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT or GL11.GL_DEPTH_BUFFER_BIT)

        if (parentPortal != null) {
            // Setup clipping plane for parent portal
            // The render is supposed to look like this from the parent (we are currently rendering Remote World):
            // Camera -> Local World -> Portal -> Remote World
            // However, we are actually rendering:
            // Camera -> Remote World -> Portal -> Remote World
            // so we need to clip away the remote world on the camera side of the portal, otherwise blocks in the
            // Remote World which are between the camera and the portal will show up in the final composed render.
            val portalPos = parentPortal.remotePosition.to3dMid()
            val cameraSide = parentPortal.remoteAxis.toFacing(cameraPos - portalPos)
            // Position clipping plane on the camera side of the portal such that the portal frame is fully rendered
            val planePos = parentPortal.remotePosition.to3dMid() + cameraSide.directionVec.to3d().scale(0.5)
            // glClipPlane uses the current ModelView matrix to transform the given coordinates to view space
            // so we need to have the camera setup before calling it
            mc.entityRenderer.setupCameraTransform(mc.renderPartialTicks, 0)
            // setupCameraTransform configures world space with the origin at the camera's feet.
            // planePos however is currently absolute world space, so we need to convert it
            val relPlanePos = planePos - cameraPos + Vec3d(0.0, mc.renderViewEntity!!.eyeHeight.toDouble(), 0.0)
            glClipPlane(GL11.GL_CLIP_PLANE5, cameraSide.directionVec.to3d().scale(-1.0), relPlanePos)
            GL11.glEnable(GL11.GL_CLIP_PLANE5) // FIXME don't hard-code clipping plane id

            // Reduce fog by distance between camera and portal, we will later re-apply this distance worth of fog
            // to the rendered portal but then with the fog of the correct dimension.
            // This won't give quite correct results for large portals but far better ones than using the incorrect fog.
            val dist = (cameraPos - portalPos).lengthVector().toFloat()
            when (GlStateManager.FogMode.values().find { it.capabilityId == GlStateManager.fogState.mode }) {
                GlStateManager.FogMode.LINEAR -> manager.fogOffset = dist
                // TODO
                else -> manager.fogOffset = 0f
            }
        }

        // Actually render the world
        val prevRenderPlan = ViewRenderPlan.CURRENT
        ViewRenderPlan.CURRENT = this
        mc.entityRenderer.renderWorld(mc.renderPartialTicks, finishTimeNano)
        ViewRenderPlan.CURRENT = prevRenderPlan

        manager.fogOffset = 0f
        GlStateManager.popMatrix()
        GlStateManager.popAttrib()
        framebuffer.unbindFramebuffer()
        world.profiler.endSection()
        return framebuffer
    }

    /**
     * Render this view and all of its dependencies.
     */
    fun render(finishTimeNano: Long): FramebufferD = try {
        renderDeps()
        renderSelf(finishTimeNano)
    } finally {
        framebuffers.values.forEach(manager::releaseFramebuffer)
        framebuffers.clear()
    }
}