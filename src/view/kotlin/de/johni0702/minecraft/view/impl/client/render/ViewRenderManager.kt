package de.johni0702.minecraft.view.impl.client.render

import de.johni0702.minecraft.betterportals.common.*
import de.johni0702.minecraft.view.client.render.*
import de.johni0702.minecraft.view.impl.ClientViewAPIImpl
import de.johni0702.minecraft.view.impl.client.ViewEntity
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.WorldClient
import net.minecraft.client.renderer.GLAllocation
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.culling.ClippingHelperImpl
import net.minecraft.client.renderer.culling.Frustum
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.client.shader.Framebuffer
import net.minecraft.util.math.Vec3d
import net.minecraftforge.client.event.DrawBlockHighlightEvent
import net.minecraftforge.client.event.EntityViewRenderEvent
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import org.lwjgl.opengl.GL11
import org.lwjgl.util.vector.Matrix4f
import org.lwjgl.util.vector.Quaternion
import kotlin.math.ceil
import kotlin.math.sqrt

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
    var realRenderDistanceChunks: Int = 16
    lateinit var debugView: () -> Boolean
    private var frameWidth = 0
    private var frameHeight = 0
    private val framebufferPool = mutableListOf<Framebuffer>()
    private val eventHandler = EventHandler()
    init {
        eventHandler.registered = true
    }
    private val disposedOcclusionQueries = mutableListOf<OcclusionQuery>()

    fun allocFramebuffer() = framebufferPool.popOrNull() ?: Framebuffer(
            frameWidth, frameHeight, true
            //#if MC>=11400
            //$$ , true
            //#endif
    ).apply {
        //#if MC>=11400
        //$$ // TODO port. but is this even necessary? aren't we using these framebuffers just for copying?
        //#else
        if (!isStencilEnabled && Minecraft.getMinecraft().framebuffer.isStencilEnabled) {
            enableStencil()
        }
        //#endif
    }

    fun releaseFramebuffer(framebuffer: Framebuffer) {
        framebufferPool.add(framebuffer)
    }

    /**
     * Determine the camera's current world, prepare all portals and render the world.
     */
    fun renderWorld(partialTicks: Float, finishTimeNano: Long) {
        val mc = Minecraft.getMinecraft()
        if (mc.player == null) {
            mc.entityRenderer.renderWorld(partialTicks, finishTimeNano)
            return
        }

        val viewManager = ClientViewAPIImpl.viewManagerImpl
        val view = viewManager.mainView

        //#if MC>=11400
        //$$ if (mc.mainWindow.framebufferWidth != frameWidth || mc.mainWindow.framebufferHeight != frameHeight) {
        //$$     frameWidth = mc.mainWindow.framebufferWidth
        //$$     frameHeight = mc.mainWindow.framebufferHeight
        //#else
        if (mc.displayWidth != frameWidth || mc.displayHeight != frameHeight) {
            frameWidth = mc.displayWidth
            frameHeight = mc.displayHeight
        //#endif
            framebufferPool.forEach { it.deleteFramebuffer() }
            framebufferPool.clear()
        }

        if (mc.renderViewEntity == null) {
            mc.renderViewEntity = mc.player
        }

        mc.mcProfiler.startSection("captureMainViewCamera")
        val viewEntity = mc.renderViewEntity!!
        val interpEntityPos = viewEntity.getPositionEyes(partialTicks)
        val cameraYaw = viewEntity.prevRotationYaw + (viewEntity.rotationYaw - viewEntity.prevRotationYaw) * partialTicks.toDouble()
        val cameraPitch = viewEntity.prevRotationPitch + (viewEntity.rotationPitch - viewEntity.prevRotationPitch) * partialTicks.toDouble()

        // Capture main view camera settings
        realRenderDistanceChunks = mc.gameSettings.renderDistanceChunks
        GlStateManager.pushMatrix()
        val camera = viewManager.withView(view) {
            eventHandler.capture = true
            mc.entityRenderer.setupCameraTransform(partialTicks
                    //#if MC<11400
                    , 0
                    //#endif
            )
            eventHandler.capture = false

            val buf = GLAllocation.createDirectFloatBuffer(16)
            //#if MC>=11400
            //$$ GL11.glGetFloatv(GL11.GL_MODELVIEW_MATRIX, buf)
            //#else
            GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, buf)
            //#endif
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
        var plan = with(DetermineRootPassEvent(this, partialTicks, view.world, camera).post()) {
            ViewRenderPlan(this@ViewRenderManager, null, this.world, this.camera)
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

        mc.mcProfiler.startSection("renderFramebuffer")
        if (debugView()) {
            plan.debugFramebuffer
        } else {
            plan.framebuffer
        }?.framebufferRender(frameWidth, frameHeight)
        mc.mcProfiler.endSection()

        plan.framebuffer?.let { releaseFramebuffer(it) }
        plan.framebuffer = null
        plan.debugFramebuffer?.let { releaseFramebuffer(it) }
        plan.debugFramebuffer = null
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

        private var currentRenderPass: ViewRenderPlan? = null
        private var currentYaw = 0f
        private var currentPitch = 0f
        private var currentRoll = 0f

        @SubscribeEvent(priority = EventPriority.LOWEST)
        fun onCameraSetup(event: EntityViewRenderEvent.CameraSetup) {
            if (capture) {
                //#if MC>=11400
                //$$ GL11.glGetFloatv(GL11.GL_PROJECTION_MATRIX, projectionMatrix)
                //$$ GL11.glGetFloatv(GL11.GL_MODELVIEW_MATRIX, modelViewMatrix)
                //#else
                GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, projectionMatrix)
                GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, modelViewMatrix)
                //#endif
                projectionMatrix.flip().limit(16) // limit(16) required as glGetFloat has no clue
                modelViewMatrix.flip().limit(16)
                yaw = event.yaw
                pitch = event.pitch
                roll = event.roll
            } else {
                val plan = ViewRenderPlan.CURRENT ?: return
                //#if MC>=11400
                //$$ GL11.glMatrixMode(GL11.GL_PROJECTION)
                //$$ GL11.glLoadMatrixf(projectionMatrix)
                //$$ GL11.glMatrixMode(GL11.GL_MODELVIEW)
                //$$ GL11.glLoadMatrixf(modelViewMatrix)
                //#else
                GL11.glMatrixMode(GL11.GL_PROJECTION)
                GL11.glLoadMatrix(projectionMatrix)
                GL11.glMatrixMode(GL11.GL_MODELVIEW)
                GL11.glLoadMatrix(modelViewMatrix)
                //#endif
                projectionMatrix.rewind()
                modelViewMatrix.rewind()

                // If this is the first camera setup for this plan, then capture the current yaw/pitch/roll
                // We do this in case yaw/pitch/roll change in a later call (e.g. because of portal gun mod or similar)
                // because then we want to add the difference on top of our calculated value instead of just overwriting
                // the (e.g.) portal gun's changes.
                if (currentRenderPass != plan) {
                    currentRenderPass = plan
                    currentYaw = event.yaw
                    currentPitch = event.pitch
                    currentRoll = event.roll
                }
                event.yaw -= currentYaw
                event.pitch -= currentPitch
                event.roll -= currentRoll

                event.yaw += yaw - mainCameraYaw + plan.camera.eyeRotation.y.toFloat()
                event.pitch += pitch - mainCameraPitch + plan.camera.eyeRotation.x.toFloat()
                event.roll += roll - mainCameraRoll + plan.camera.eyeRotation.z.toFloat()
            }
        }

        //#if MC>=11400
        //$$ private var fov: Double = 0.0
        //#else
        private var fov: Float = 0.toFloat()
        //#endif
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
            // Render block outlines only in main view (where the player entity is located)
            if (Minecraft.getMinecraft().player is ViewEntity) {
                event.isCanceled = true
            }
        }
    }
}

internal class ViewRenderPlan(
        override val manager: ViewRenderManager,
        override val parent: RenderPass?,
        override val world: WorldClient,
        override val camera: Camera
) : RenderPass {
    companion object {
        var MAIN: ViewRenderPlan? = null
        var CURRENT: ViewRenderPlan? = null
        var PREVIOUS_FRAME: ViewRenderPlan? = null
    }
    override var framebuffer: Framebuffer? = null
    var debugFramebuffer: Framebuffer? = null

    override val children = mutableListOf<ViewRenderPlan>()

    override fun addChild(world: WorldClient, camera: Camera, previousFrame: RenderPass?): ViewRenderPlan {
        val child = ViewRenderPlan(manager, this, world, camera)
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
        renderDistanceDetail = RenderDistanceDetail()
        chunkVisibilityDetail = ChunkVisibilityDetail()
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
    private fun renderSelf(partialTicks: Float, finishTimeNano: Long): Framebuffer {
        if (CURRENT != this) {
            val prev = CURRENT
            CURRENT = this
            try {
                return renderSelf(partialTicks, finishTimeNano)
            } finally {
                CURRENT = prev
            }
        }

        val view = ClientViewAPIImpl.viewManagerImpl.views.find { it.world == world }!!
        if (view.manager.activeView != view) {
            return view.manager.withView(view) {
                renderSelf(partialTicks, finishTimeNano)
            }
        }
        val mc = Minecraft.getMinecraft()

        // Render GUI only in main view
        if (!mc.gameSettings.hideGUI && mc.player is ViewEntity) {
            mc.gameSettings.hideGUI = true
            try {
                return renderSelf(partialTicks, finishTimeNano)
            } finally {
                mc.gameSettings.hideGUI = false
            }
        }

        val framebuffer = manager.allocFramebuffer()
        this.framebuffer = framebuffer

        world.profiler.startSection("renderWorld" + world.provider.dimension)

        // Inject the entity from which the world will be rendered
        // We do not spawn it into the world as we don't need it there (until some third-party mod does)
        val orgViewEntity = mc.renderViewEntity ?: mc.player
        val interpEntityPos = orgViewEntity.getPositionEyes(partialTicks)
        // Unless this is the first person view
        if (mc.player is ViewEntity || mc.gameSettings.thirdPersonView > 0 || !interpEntityPos.approxEquals(camera.eyePosition, 1e-4)) {
            val cameraEntity = ViewCameraEntity(mc.world)
            with(camera) {
                cameraEntity.pos = feetPosition
                cameraEntity.prevPos = feetPosition
                cameraEntity.lastTickPos = feetPosition
                cameraEntity.rotationYaw = eyeRotation.y.toFloat()
                cameraEntity.prevRotationYaw = eyeRotation.y.toFloat()
                cameraEntity.rotationPitch = eyeRotation.x.toFloat()
                cameraEntity.prevRotationPitch = eyeRotation.x.toFloat()
                //#if MC>=11400
                //$$ cameraEntity.eyeHeightOverwrite = (eyePosition.y - feetPosition.y).toFloat()
                //$$ cameraEntity.recalculateSize()
                //#else
                cameraEntity.eyeHeight = (eyePosition.y - feetPosition.y).toFloat()
                //#endif
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
        //      Note: still need it for 0.0 render distance optimization
        GlStateManager.disableFog()
        GlStateManager.disableLighting()
        mc.entityRenderer.disableLightmap()
        //#if MC>=11400
        //$$ mc.gameRenderer.fogRenderer.updateFogColor(mc.gameRenderer.activeRenderInfo, partialTicks)
        //#else
        mc.entityRenderer.updateFogColor(partialTicks)
        //#endif
        GL11.glClearDepth(1.0)
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT or GL11.GL_DEPTH_BUFFER_BIT)

        // MC only dirties it (and recalculates visible chunks as a consequence) when the camera has moved (pos or rot).
        // Our camera doesn't move but our view frustum changes and for that visible chunks have to be updated.
        mc.renderGlobal.setDisplayListEntitiesDirty()

        if (!haveCubicChunks) { // TODO: CC does its own check which we currently do not hook into (it also has vertical render distance)
            renderDistanceDetail.renderDistanceChunks?.let { mc.gameSettings.renderDistanceChunks = it }
        }

        RenderPassEvent.Start(partialTicks, this).post()

        // Actually render the world
        if (renderDistanceDetail.renderDistance != 0.0) {
            mc.entityRenderer.renderWorld(partialTicks, finishTimeNano)
        }

        RenderPassEvent.End(partialTicks, this).post()

        mc.renderViewEntity = orgViewEntity
        mc.gameSettings.renderDistanceChunks = manager.realRenderDistanceChunks

        GlStateManager.popMatrix()
        framebuffer.unbindFramebuffer()
        mc.framebufferMc = framebufferMc
        world.profiler.endSection()
        return framebuffer
    }

    private fun renderDebug() {
        val finalFramebuffer = framebuffer ?: return
        val total = 1 + children.size
        val side = ceil(sqrt(total.toDouble())).toInt()
        val tmp = manager.allocFramebuffer()

        tmp.bindFramebuffer(false)
        GlStateManager.colorMask(true, true, true, false)
        GlStateManager.disableDepth()
        GlStateManager.depthMask(false)
        GlStateManager.matrixMode(GL11.GL_PROJECTION)
        GlStateManager.loadIdentity()
        GlStateManager.ortho(0.0, tmp.framebufferWidth.toDouble(), tmp.framebufferHeight.toDouble(), 0.0, 1000.0, 3000.0)
        GlStateManager.matrixMode(GL11.GL_MODELVIEW)
        GlStateManager.loadIdentity()
        GlStateManager.translate(0.0F, 0.0F, -2000.0F)
        GlStateManager.enableTexture2D()
        GlStateManager.disableLighting()
        GlStateManager.disableAlpha()
        GlStateManager.disableBlend()
        GlStateManager.enableColorMaterial()
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F)

        var idx = 0
        for (row in 0 until side) {
            for (column in 0 until side) {
                val fb = if (row == 0 && column == 0) {
                    finalFramebuffer
                } else {
                    children.getOrNull(idx++)?.debugFramebuffer
                }

                fb?.bindFramebufferTexture()
                val x0 = column.toDouble() * tmp.framebufferWidth / side
                val y0 = row.toDouble() * tmp.framebufferHeight / side
                val x1 = x0 + tmp.framebufferWidth / side
                val y1 = y0 + tmp.framebufferHeight / side
                val tw = tmp.framebufferWidth / tmp.framebufferTextureWidth.toDouble()
                val th = tmp.framebufferHeight / tmp.framebufferTextureHeight.toDouble()
                val tessellator = Tessellator.getInstance()
                with(tessellator.buffer) {
                    begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR)
                    pos(x0, y1, 0.0).tex(0.0, 0.0).color(255, 255, 255, 255).endVertex()
                    pos(x1, y1, 0.0).tex(tw, 0.0).color(255, 255, 255, 255).endVertex()
                    pos(x1, y0, 0.0).tex(tw, th).color(255, 255, 255, 255).endVertex()
                    pos(x0, y0, 0.0).tex(0.0, th).color(255, 255, 255, 255).endVertex()
                }
                tessellator.draw()
                fb?.unbindFramebufferTexture()
            }
        }

        GlStateManager.depthMask(true)
        GlStateManager.colorMask(true, true, true, true)
        tmp.unbindFramebuffer()

        debugFramebuffer = tmp
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
            renderDebug()
        } finally {
            children.forEach {
                manager.releaseFramebuffer(it.framebuffer ?: return@forEach)
                it.framebuffer = null
            }
            children.forEach {
                manager.releaseFramebuffer(it.debugFramebuffer ?: return@forEach)
                it.debugFramebuffer = null
            }
        }
    }
}