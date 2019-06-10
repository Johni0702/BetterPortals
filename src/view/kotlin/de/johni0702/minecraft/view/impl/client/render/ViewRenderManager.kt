package de.johni0702.minecraft.view.impl.client.render

import de.johni0702.minecraft.betterportals.common.*
import de.johni0702.minecraft.view.client.ClientView
import de.johni0702.minecraft.view.client.render.*
import de.johni0702.minecraft.view.client.viewManager
import de.johni0702.minecraft.view.impl.client.ViewEntity
import de.johni0702.minecraft.view.impl.compat.Optifine
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GLAllocation
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.Matrix4f
import net.minecraft.client.renderer.culling.ClippingHelperImpl
import net.minecraft.client.renderer.culling.Frustum
import net.minecraft.util.math.Vec3d
import net.minecraft.world.World
import net.minecraftforge.client.event.DrawBlockHighlightEvent
import net.minecraftforge.client.event.EntityViewRenderEvent
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import org.lwjgl.opengl.GL11
import org.lwjgl.util.vector.Quaternion

internal class ViewRenderManager : RenderPassManager {
    override val root: RenderPass?
        get() = ViewRenderPlan.MAIN
    override val current: RenderPass?
        get() = ViewRenderPlan.CURRENT
    override val previous: RenderPass?
        get() = ViewRenderPlan.PREVIOUS_FRAME

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
    private val disposedOcclusionQueries = mutableListOf<OcclusionQuery>()

    fun allocFramebuffer() = framebufferPool.popOrNull() ?: FramebufferD(frameWidth, frameHeight).apply {
        if (!isStencilEnabled && Minecraft.getMinecraft().framebuffer.isStencilEnabled) {
            enableStencil()
        }
    }

    fun releaseFramebuffer(framebuffer: FramebufferD) {
        framebufferPool.add(framebuffer)
    }

    /**
     * Determine the camera's current world, prepare all portals and render the world.
     */
    fun renderWorld(partialTicks: Float, finishTimeNano: Long) {
        val mc = Minecraft.getMinecraft()
        val view = mc.viewManager?.mainView
        if (view == null) {
            mc.entityRenderer.renderWorld(partialTicks, finishTimeNano)
            return
        }

        if (mc.displayWidth != frameWidth || mc.displayHeight != frameHeight) {
            frameWidth = mc.displayWidth
            frameHeight = mc.displayHeight
            framebufferPool.forEach { it.deleteFramebuffer() }
            framebufferPool.clear()
        }

        mc.mcProfiler.startSection("captureMainViewCamera")
        val viewEntity = mc.renderViewEntity ?: mc.player
        val interpEntityPos = viewEntity.getPositionEyes(partialTicks)
        val cameraYaw = viewEntity.prevRotationYaw + (viewEntity.rotationYaw - viewEntity.prevRotationYaw) * partialTicks.toDouble()
        val cameraPitch = viewEntity.prevRotationPitch + (viewEntity.rotationPitch - viewEntity.prevRotationPitch) * partialTicks.toDouble()

        // Capture main view camera settings
        GlStateManager.pushMatrix()
        val camera = view.withView {
            eventHandler.capture = true
            mc.entityRenderer.setupCameraTransform(partialTicks, 0)
            eventHandler.capture = false

            val buf = GLAllocation.createDirectFloatBuffer(16)
            GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, buf)
            buf.flip().limit(16)
            val mat = Matrix4f().apply { load(buf) }
            val inv = mat.inverse
            val viewPosOffset = Vec3d(inv.m30.toDouble(), inv.m31.toDouble(), inv.m32.toDouble())
            val viewRot = Quaternion.setFromMatrix(mat, Quaternion()).apply { normalise() }.toPitchYawRoll()

            val feetPos = interpEntityPos - viewEntity.eyeOffset
            val viewPos = feetPos + viewPosOffset

            eventHandler.mainCameraYaw = cameraYaw.toFloat()
            eventHandler.mainCameraPitch = cameraPitch.toFloat()

            val frustum = Frustum(ClippingHelperImpl().apply { init() }).apply {
                with(feetPos) { setPosition(x, y, z) }
            }
            Camera(
                    frustum,
                    feetPos,
                    interpEntityPos,
                    viewPos,
                    Vec3d(cameraPitch, cameraYaw, 0.0),
                    viewRot
            )
        }
        GlStateManager.popMatrix()


        mc.mcProfiler.endStartSection("pollOcclusionQueries")

        // Update occlusion queries
        val activeOcclusionDetails = mutableSetOf<OcclusionDetail>()
        fun updateOcclusionQueries(plan: ViewRenderPlan) {
            val detail = plan.occlusionDetail
            activeOcclusionDetails.add(detail)
            val accessed = detail.accessed
            val query = detail.occlusionQuery
            query.update()
            detail.occluded = accessed && query.occluded
            detail.accessed = false

            plan.children.forEach(::updateOcclusionQueries)
        }
        ViewRenderPlan.PREVIOUS_FRAME?.let { updateOcclusionQueries(it) }

        mc.mcProfiler.endStartSection("determineRootRenderPass")

        // Build render plan
        var plan = with(DetermineRootPassEvent(this, partialTicks, view, camera).post()) {
            ViewRenderPlan(this@ViewRenderManager, null, this.view, this.camera)
        }

        mc.mcProfiler.endStartSection("populateRenderPassTree")

        do {
            val event = PopulateTreeEvent(partialTicks, plan, false).post()
            plan = event.root as ViewRenderPlan
        } while (event.changed)

        mc.mcProfiler.endStartSection("cleanupOcclusionQueries")

        // Cleanup occlusion queries for portals which are no longer visible
        fun cleanupOcclusionQueries(plan: ViewRenderPlan) {
            if (!activeOcclusionDetails.contains(plan.occlusionDetail)) {
                disposedOcclusionQueries.add(plan.occlusionDetail.occlusionQuery)
            }
            plan.children.forEach(::updateOcclusionQueries)
        }
        ViewRenderPlan.PREVIOUS_FRAME?.let(::cleanupOcclusionQueries)
        disposedOcclusionQueries.removeIf { it.update() }

        mc.mcProfiler.endSection()

        // execute
        mc.framebuffer.unbindFramebuffer()
        ViewRenderPlan.MAIN = plan
        plan.render(partialTicks, finishTimeNano)
        ViewRenderPlan.MAIN = null
        ViewRenderPlan.PREVIOUS_FRAME = plan
        mc.framebuffer.bindFramebuffer(true)

        val framebuffer = plan.framebuffer ?: return

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
        private var projectionMatrix = GLAllocation.createDirectFloatBuffer(16)
        private var modelViewMatrix = GLAllocation.createDirectFloatBuffer(16)
        private var yaw = 0.toFloat()
        private var pitch = 0.toFloat()
        private var roll = 0.toFloat()

        @SubscribeEvent(priority = EventPriority.LOWEST)
        fun onCameraSetup(event: EntityViewRenderEvent.CameraSetup) {
            if (capture) {
                GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, projectionMatrix)
                GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, modelViewMatrix)
                projectionMatrix.flip().limit(16) // limit(16) required as glGetFloat has no clue
                modelViewMatrix.flip().limit(16)
                yaw = event.yaw
                pitch = event.pitch
                roll = event.roll
            } else {
                val plan = ViewRenderPlan.CURRENT ?: return
                GL11.glMatrixMode(GL11.GL_PROJECTION)
                GL11.glLoadMatrix(projectionMatrix)
                GL11.glMatrixMode(GL11.GL_MODELVIEW)
                GL11.glLoadMatrix(modelViewMatrix)
                projectionMatrix.rewind()
                modelViewMatrix.rewind()
                event.yaw = yaw - mainCameraYaw + plan.camera.eyeRotation.y.toFloat()
                event.pitch = pitch - mainCameraPitch + plan.camera.eyeRotation.x.toFloat()
                event.roll = roll - mainCameraRoll + plan.camera.eyeRotation.z.toFloat()
            }
        }

        private var fov: Float = 0.toFloat()
        @SubscribeEvent(priority = EventPriority.LOWEST)
        fun onFOVSetup(event: EntityViewRenderEvent.FOVModifier) {
            if (capture) {
                fov = event.fov
            } else if (Minecraft.getMinecraft().player is ViewEntity) {
                // MC uses a different fov for rendering the hand than for the rest but we can't know which the current
                // event is meant for. Since the hand is only rendered for non-view entities (i.e. the main view) and
                // the same view is also the one which we record the fov from, we just never modify the fov for it.
                event.fov = fov
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

internal class ViewRenderPlan(
        override val manager: ViewRenderManager,
        override val parent: RenderPass?,
        override val view: ClientView,
        override val camera: Camera
) : RenderPass {
    companion object {
        var MAIN: ViewRenderPlan? = null
        var CURRENT: ViewRenderPlan? = null
        var PREVIOUS_FRAME: ViewRenderPlan? = null
    }
    val world: World = view.camera.world
    override var framebuffer: FramebufferD? = null

    override val children = mutableListOf<ViewRenderPlan>()

    override fun addChild(view: ClientView, camera: Camera, previousFrame: RenderPass?): ViewRenderPlan {
        val child = ViewRenderPlan(manager, this, view, camera)
        if (previousFrame != null) {
            child.occlusionDetail = previousFrame.occlusionDetail.also {
                previousFrame.occlusionDetail = child.occlusionDetail
            }
        }
        children.add(child)
        return child
    }

    private val details = mutableMapOf<Class<*>, Any>()
    init {
        occlusionDetail = OcclusionDetail(OcclusionQuery())
    }

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

        if (CURRENT != this) {
            val prev = CURRENT
            CURRENT = this
            try {
                return renderSelf(partialTicks, finishTimeNano)
            } finally {
                CURRENT = prev
            }
        }

        if (view.manager.activeView != view) {
            return view.withView { renderSelf(partialTicks, finishTimeNano) }
        }
        val mc = Minecraft.getMinecraft()

        // Render GUI only in main view
        if (!mc.gameSettings.hideGUI && !view.isMainView) {
            mc.gameSettings.hideGUI = true
            try {
                return renderSelf(partialTicks, finishTimeNano)
            } finally {
                mc.gameSettings.hideGUI = false
            }
        }

        val framebuffer = manager.allocFramebuffer()
        this.framebuffer = framebuffer

        world.profiler.startSection("renderView" + view.id)

        // Inject the entity from which the world will be rendered
        // We do not spawn it into the world as we don't need it there (until some third-party mod does)
        if (mc.player is ViewEntity) {
            val cameraEntity = ViewCameraEntity(mc.world)
            with(camera) {
                cameraEntity.pos = feetPosition
                cameraEntity.prevPos = feetPosition
                cameraEntity.lastTickPos = feetPosition
                cameraEntity.eyeHeight = (eyePosition.y - feetPosition.y).toFloat()
                cameraEntity.rotationYaw = eyeRotation.y.toFloat()
                cameraEntity.prevRotationYaw = eyeRotation.y.toFloat()
                cameraEntity.rotationPitch = eyeRotation.x.toFloat()
                cameraEntity.prevRotationPitch = eyeRotation.x.toFloat()
            }
            mc.renderViewEntity = cameraEntity
        }

        // Bind framebuffer and temporarily replace MC's one
        val framebufferMc = mc.framebufferMc
        mc.framebufferMc = framebuffer
        framebuffer.bindFramebuffer(false)
        GlStateManager.pushMatrix()

        // Clear framebuffer
        // TODO given we move the fog calculations into some mixin, we should no longer need this block
        GlStateManager.disableFog()
        GlStateManager.disableLighting()
        mc.entityRenderer.disableLightmap()
        mc.entityRenderer.updateFogColor(partialTicks)
        GL11.glClearDepth(1.0)
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT or GL11.GL_DEPTH_BUFFER_BIT)

        // MC only dirties it (and recalculates visible chunks as a consequence) when the camera has moved (pos or rot).
        // Our camera doesn't move but our view frustum changes and for that visible chunks have to be updated.
        mc.renderGlobal.setDisplayListEntitiesDirty()

        RenderPassEvent.Start(partialTicks, this).post()

        // Actually render the world
        mc.entityRenderer.renderWorld(partialTicks, finishTimeNano)

        RenderPassEvent.End(partialTicks, this).post()

        if (mc.player is ViewEntity) {
            mc.renderViewEntity = mc.player
        }

        GlStateManager.popMatrix()
        framebuffer.unbindFramebuffer()
        mc.framebufferMc = framebufferMc
        world.profiler.endSection()
        return framebuffer
    }

    /**
     * Render this view and all of its dependencies.
     */
    fun render(partialTicks: Float, finishTimeNano: Long) {
        try {
            val prepareEvent = RenderPassEvent.Prepare(partialTicks, this)
            if (occlusionDetail.occluded) {
                prepareEvent.isCanceled = true
            }
            if (prepareEvent.post().isCanceled) return
            renderDeps(partialTicks)
            if (RenderPassEvent.Before(partialTicks, this).post().isCanceled) return
            renderSelf(partialTicks, finishTimeNano)
            RenderPassEvent.After(partialTicks, this).post()
        } finally {
            children.forEach {
                manager.releaseFramebuffer(it.framebuffer ?: return@forEach)
                it.framebuffer = null
            }
        }
    }
}