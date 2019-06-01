package de.johni0702.minecraft.betterportals.client.renderer

import de.johni0702.minecraft.betterportals.BPConfig
import de.johni0702.minecraft.betterportals.BetterPortalsMod
import de.johni0702.minecraft.betterportals.client.*
import de.johni0702.minecraft.betterportals.client.compat.Optifine
import de.johni0702.minecraft.view.client.ClientView
import de.johni0702.minecraft.betterportals.client.view.ClientViewImpl
import de.johni0702.minecraft.betterportals.common.*
import de.johni0702.minecraft.betterportals.common.entity.AbstractPortalEntity
import de.johni0702.minecraft.view.client.render.Camera
import de.johni0702.minecraft.view.client.render.RenderPass
import de.johni0702.minecraft.view.client.render.RenderPassManager
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.culling.Frustum
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.Vec3d
import net.minecraft.world.World
import net.minecraftforge.client.event.DrawBlockHighlightEvent
import net.minecraftforge.client.event.EntityViewRenderEvent
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import org.lwjgl.opengl.GL11

class ViewRenderManager : RenderPassManager {
    override val root: RenderPass?
        get() = ViewRenderPlan.MAIN
    override val current: RenderPass?
        get() = ViewRenderPlan.CURRENT

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

    fun allocFramebuffer() = framebufferPool.popOrNull() ?: FramebufferD(frameWidth, frameHeight).apply {
        if (!isStencilEnabled && Minecraft.getMinecraft().framebuffer.isStencilEnabled) {
            enableStencil()
        }
    }

    fun releaseFramebuffer(framebuffer: FramebufferD) {
        framebufferPool.add(framebuffer)
    }

    fun getOcclusionQuery(entity: AbstractPortalEntity) = occlusionQueries.getOrPut(entity, ::OcclusionQuery)

    /**
     * Determine the camera's current world, prepare all portals and render the world.
     */
    fun renderWorld(partialTicks: Float, finishTimeNano: Long) {
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
        val entityPos = viewEntity.syncPos + viewEntity.eyeOffset
        val interpEntityPos = viewEntity.getPositionEyes(partialTicks)
        // TODO do third person camera
        var cameraPos = interpEntityPos
        var cameraYaw = viewEntity.prevRotationYaw + (viewEntity.rotationYaw - viewEntity.prevRotationYaw) * partialTicks.toDouble()
        var cameraPitch = viewEntity.prevRotationPitch + (viewEntity.rotationPitch - viewEntity.prevRotationPitch) * partialTicks.toDouble()

        var parentPortal: AbstractPortalEntity? = null

        // Ray trace from the entity (eye) position backwards to the camera position, following any portals which therefore
        // the camera must be looking through.
        // First back through time from the actual entity position to the interpolated (visual) position in this frame
        // (which may very well still be in the previous world, then through space from the visual position to where
        // the camera is positioned.
        var pos = entityPos
        var target = interpEntityPos
        var hitVisualPosition = false
        while (true) {
            val hitInfo = view.camera.world.getEntities(AbstractPortalEntity::class.java) {
                val view = it?.view
                // FIXME handle one-way portals
                // Ignore portals which haven't yet been loaded or have already been destroyed
                view != null && !it.isDead
                        // or have already been used in the previous iteration
                        && (view.camera.world != parentPortal?.world || it.localPosition != parentPortal?.remotePosition)
            }.flatMap { portal ->
                // For each portal, find the point intercepting the line between entity and camera
                val vec = portal.localFacing.directionVec.to3d() * 0.5
                val negVec = vec * -1
                portal.localBlocks.map {
                    // contract BB to only detect changes crossing 0.5 on the portal axis instead of hitting anywhere in the block
                    val trace = AxisAlignedBB(it).contract(vec).contract(negVec).calculateIntercept(pos, target)
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

                // If we hit a portal, switch to its view and transform the camera/entity positions accordingly
                // also change the current position to be in the portal so we don't accidentally match any portals
                // behind the one we're looking through.
                view = portal.view!!
                target = (portal.localToRemoteMatrix * target.toPoint()).toMC()
                cameraPos = (portal.localToRemoteMatrix * cameraPos.toPoint()).toMC()
                cameraYaw += (portal.remoteRotation - portal.localRotation).degrees.toDouble()
                pos = (portal.localToRemoteMatrix * hitVec.toPoint()).toMC()
                parentPortal = portal
            } else if (!hitVisualPosition) {
                hitVisualPosition = true
                pos = target
                target = cameraPos
            } else {
                break
            }
        }

        mc.mcProfiler.endSection()

        // Capture camera properties (rotation, fov)
        GlStateManager.pushMatrix()
        eventHandler.capture = true
        eventHandler.mainCameraYaw = cameraYaw.toFloat()
        eventHandler.mainCameraPitch = cameraYaw.toFloat()
        val camera = view.withView {
            mc.entityRenderer.setupCameraTransform(partialTicks, 0)
            val entity = mc.renderViewEntity!!
            val entityPos = entity.lastTickPos + (entity.pos - entity.lastTickPos) * partialTicks.toDouble()
            Frustum().apply { setPosition(entityPos.x, entityPos.y, entityPos.z) }
        }
        eventHandler.capture = false
        GlStateManager.popMatrix()

        val maxRecursions = if (BPConfig.seeThroughPortals) 5 else 0

        // Build render plan
        val plan = ViewRenderPlan(this, null, view, Camera(camera, cameraPos, Vec3d(cameraYaw, cameraPitch, 0.0)))

        // Recursively add portal views
        plan.addPortals(maxRecursions)

        // Update occlusion queries
        occlusionQueries.values.forEach { it.update() }

        // Cleanup occlusion queries for portals which are no longer visible
        fun getAllPortals(plan: ViewRenderPlan, result: MutableSet<AbstractPortalEntity>) {
            plan.portalDetail?.parent?.let { result.add(it) }
            plan.children.forEach { getAllPortals(it, result) }
        }
        val knownPortals = mutableSetOf<AbstractPortalEntity>()
        getAllPortals(plan, knownPortals)
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
        ViewRenderPlan.MAIN = plan
        val framebuffer = plan.render(partialTicks, finishTimeNano)
        ViewRenderPlan.MAIN = null
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
        var mainCameraPitch = 0.toFloat()
        var mainCameraRoll = 0.toFloat()
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
                event.yaw = yaw - mainCameraYaw + plan.camera.rotation.y.toFloat()
                event.pitch = pitch - mainCameraPitch + plan.camera.rotation.x.toFloat()
                event.roll = roll - mainCameraRoll + plan.camera.rotation.z.toFloat()
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

        @SubscribeEvent(priority = EventPriority.LOW)
        fun onRenderBlockHighlights(event: DrawBlockHighlightEvent) {
            val plan = ViewRenderPlan.CURRENT ?: return
            // Render block outlines only in main view (where the player entity is located)
            if (!plan.view.isMainView) {
                event.isCanceled = true
            }
        }
    }
}

class ViewRenderPlan(
        override val manager: ViewRenderManager,
        override val parent: RenderPass?,
        override val view: ClientView,
        override val camera: Camera
) : RenderPass {
    companion object {
        var MAIN: ViewRenderPlan? = null
        var CURRENT: ViewRenderPlan? = null
    }
    val world: World = view.camera.world
    override var framebuffer: FramebufferD? = null

    override val children = mutableListOf<ViewRenderPlan>()

    override fun addChild(view: ClientView, camera: Camera): ViewRenderPlan {
        val child = ViewRenderPlan(manager, this, view, camera)
        children.add(child)
        return child
    }

    fun addPortals(maxRecursions: Int) {
        if (maxRecursions <= 0) return
        val parentPortal = portalDetail?.parent
        world.getEntities(AbstractPortalEntity::class.java) {
            // portal must be visible (i.e. must not be frustum culled)
            it!!.canBeSeen(camera.frustum)
                    // its view must have been loaded (otherwise there's nothing to render)
                    && it.view != null
                    // it must not be our parent (the portal from which this world is being viewed)
                    // that is, it must either link to a different world or to a different place than our parent portal
                    && (it.view != parentPortal?.view || it.remotePosition != parentPortal?.localPosition)
                    // it must not be occluded by blocks
                    && !manager.getOcclusionQuery(it).occluded
        }.forEach { portal ->
            val rotation = portal.remoteRotation - portal.localRotation
            val cameraRot = camera.rotation + Vec3d(0.0, rotation.degrees.toDouble(), 0.0)
            val cameraPos = (portal.localToRemoteMatrix * camera.position.toPoint()).toMC()
            val frustum = PortalCamera(portal, cameraPos, camera.frustum)
            val camera = Camera(frustum, cameraPos, cameraRot)
            val plan = addChild(portal.view!!, camera)
            plan.portalDetail = PortalDetail(portal)
            plan.addPortals(maxRecursions - 1)
        }
    }

    private val details = mutableMapOf<Class<*>, Any>()

    override fun <T> set(type: Class<T>, detail: T?) {
        details[type] = detail as Any
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> get(type: Class<T>): T? = details[type] as T?

    /**
     * Render all dependencies of this view (including transitive ones).
     */
    private fun renderDeps(partialTicks: Float) {
        children.forEach {
            it.portalDetail?.parent?.onUpdate()
            it.render(partialTicks, 0)
        }
    }

    /**
     * Render this view.
     * Requires all dependencies to have previously been rendered (e.g. by calling [renderDeps]), otherwise their
     * portals will be empty.
     */
    private fun renderSelf(partialTicks: Float, finishTimeNano: Long): FramebufferD {
        // Optifine reloads its shader when the dimension changes, so for now, when shaders are enabled, we can only
        // render the main view.
        if (Optifine?.shadersActive == true && this != MAIN) {
            val framebuffer = manager.allocFramebuffer()
            this.framebuffer = framebuffer
            framebuffer.bindFramebuffer(false)
            GL11.glClearColor(0f, 0f, 0f, 1f)
            GL11.glClearDepth(1.0)
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT or GL11.GL_DEPTH_BUFFER_BIT)
            framebuffer.unbindFramebuffer()
            return framebuffer
        }

        if (view.manager.activeView != view) {
            return view.withView { renderSelf(partialTicks, finishTimeNano) }
        }
        val mc = Minecraft.getMinecraft()
        val framebuffer = manager.allocFramebuffer()
        this.framebuffer = framebuffer

        world.profiler.startSection("renderView" + view.id)

        val framebufferMc = mc.framebufferMc
        mc.framebufferMc = framebuffer
        framebuffer.bindFramebuffer(false)
        GlStateManager.pushMatrix()

        // Clear framebuffer
        GlStateManager.disableFog()
        GlStateManager.disableLighting()
        mc.entityRenderer.disableLightmap()
        mc.entityRenderer.updateFogColor(partialTicks)
        GL11.glClearDepth(1.0)
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT or GL11.GL_DEPTH_BUFFER_BIT)

        val parentPortal = portalDetail?.parent
        if (parentPortal != null) {
            // Setup clipping plane for parent portal
            // The render is supposed to look like this from the parent (we are currently rendering Remote World):
            // Camera -> Local World -> Portal -> Remote World
            // However, we are actually rendering:
            // Camera -> Remote World -> Portal -> Remote World
            // so we need to clip away the remote world on the camera side of the portal, otherwise blocks in the
            // Remote World which are between the camera and the portal will show up in the final composed render.
            val portalPos = parentPortal.remotePosition.to3dMid()
            val cameraSide = parentPortal.remoteAxis.toFacing(camera.position - portalPos)
            // Position clipping plane on the camera side of the portal such that the portal frame is fully rendered
            val planePos = parentPortal.remotePosition.to3dMid() + cameraSide.directionVec.to3d().scale(0.5)
            // glClipPlane uses the current ModelView matrix to transform the given coordinates to view space
            // so we need to have the camera setup before calling it
            mc.entityRenderer.setupCameraTransform(partialTicks, 0)
            // setupCameraTransform configures world space with the origin at the camera's feet.
            // planePos however is currently absolute world space, so we need to convert it
            val relPlanePos = planePos - camera.position + mc.renderViewEntity!!.eyeOffset
            glClipPlane(GL11.GL_CLIP_PLANE5, cameraSide.directionVec.to3d().scale(-1.0), relPlanePos)
            GL11.glEnable(GL11.GL_CLIP_PLANE5) // FIXME don't hard-code clipping plane id

            // Reduce fog by distance between camera and portal, we will later re-apply this distance worth of fog
            // to the rendered portal but then with the fog of the correct dimension.
            // This won't give quite correct results for large portals but far better ones than using the incorrect fog.
            val dist = (camera.position - portalPos).lengthVector().toFloat()
            when (GlStateManager.FogMode.values().find { it.capabilityId == GlStateManager.fogState.mode }) {
                GlStateManager.FogMode.LINEAR -> manager.fogOffset = dist
                // TODO
                else -> manager.fogOffset = 0f
            }
        }

        // Actually render the world
        val prevRenderPlan = ViewRenderPlan.CURRENT
        ViewRenderPlan.CURRENT = this
        mc.entityRenderer.renderWorld(partialTicks, finishTimeNano)
        ViewRenderPlan.CURRENT = prevRenderPlan

        manager.fogOffset = 0f
        GlStateManager.popMatrix()
        GL11.glDisable(GL11.GL_CLIP_PLANE5) // FIXME don't hard-code clipping plane id
        framebuffer.unbindFramebuffer()
        mc.framebufferMc = framebufferMc
        world.profiler.endSection()
        return framebuffer
    }

    /**
     * Render this view and all of its dependencies.
     */
    fun render(partialTicks: Float, finishTimeNano: Long): FramebufferD = try {
        renderDeps(partialTicks)
        MinecraftForge.EVENT_BUS.post(PreRenderView(this, partialTicks))
        renderSelf(partialTicks, finishTimeNano)
    } finally {
        children.forEach {
            manager.releaseFramebuffer(it.framebuffer ?: return@forEach)
            it.framebuffer = null
        }
    }
}