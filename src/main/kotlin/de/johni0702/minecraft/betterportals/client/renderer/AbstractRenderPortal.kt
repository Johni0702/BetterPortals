package de.johni0702.minecraft.betterportals.client.renderer

import de.johni0702.minecraft.betterportals.client.compat.Optifine
import de.johni0702.minecraft.betterportals.client.glClipPlane
import de.johni0702.minecraft.betterportals.common.*
import de.johni0702.minecraft.betterportals.common.entity.AbstractPortalEntity
import net.minecraft.client.Minecraft
import net.minecraft.client.entity.EntityPlayerSP
import net.minecraft.client.renderer.BufferBuilder
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.culling.Frustum
import net.minecraft.client.renderer.entity.Render
import net.minecraft.client.renderer.entity.RenderManager
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher.*
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.client.shader.ShaderManager
import net.minecraft.entity.Entity
import net.minecraft.util.EnumFacing
import net.minecraft.util.ResourceLocation
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.Vec3d
import org.lwjgl.opengl.GL11
import kotlin.math.sign

abstract class AbstractRenderPortal<T : AbstractPortalEntity>(renderManager: RenderManager) : Render<T>(renderManager) {

    companion object {

        private val mc: Minecraft = Minecraft.getMinecraft()

        private val stencilStack = mutableListOf<Boolean>()
        private val clippingStack = mutableListOf<Boolean>()

        fun beforeRender(renderManager: RenderManager, entity: Entity, partialTicks: Float): Boolean {
            if (entity is Portal) return true
            val portal = ViewRenderPlan.CURRENT?.let { instance ->
                // If we're not rendering our own world (i.e. if we're looking through a portal)
                // then we do not want to render entities on the wrong remote side of said portal
                val portal = instance.parentPortal ?: return@let null
                val portalPos = portal.remotePosition.to3dMid()
                val facing = portal.remoteFacing.axis.toFacing(instance.cameraPos - portalPos)
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
            val inPortals = entity.world.getEntities(AbstractPortalEntity::class.java) {
                it != null
                        // the portal has to be alive
                        && !it.isDead
                        // and not the remote end of our current one
                        && it.localPosition != portal?.remotePosition
                        // the entity has to be even remotely close to it
                        && it.localBoundingBox.intersects(entityAABB)
                        // if it is, then check if it's actually in one of the blocks (and not some hole)
                        && it.localBlocks.any { block -> AxisAlignedBB(block).intersects(entityAABB) }
            }
            // FIXME can't deal with entities which are in more than one portal at the same time
            inPortals.firstOrNull()?.let {
                val entityPos = entity.syncPos + entity.eyeOffset
                val relativePosition = entityPos - it.localPosition.to3d().addVector(0.5, 0.0, 0.5)
                val portalFacing = it.localFacing
                val portalDir = portalFacing.directionVec.to3d()
                val planeDir = portalDir.scale(sign(relativePosition.dotProduct(portalDir)))
                val portalX = it.posX - staticPlayerX
                val portalY = it.posY - staticPlayerY
                val portalZ = it.posZ - staticPlayerZ
                val renderer = renderManager.getEntityRenderObject<AbstractPortalEntity>(it) as AbstractRenderPortal
                val planeOffset = renderer.createInstance(it, portalX, portalY, portalZ, partialTicks).viewFacing.directionVec.to3d().scale(-0.5)
                val planePos = Vec3d(portalX, portalY, portalZ) + planeOffset
                glClipPlane(GL11.GL_CLIP_PLANE4, planeDir, planePos)
                GL11.glEnable(GL11.GL_CLIP_PLANE4) // FIXME don't hard-code clipping plane id
            }
            clippingStack.add(inPortals.isNotEmpty())

            GL11.glDisable(GL11.GL_CLIP_PLANE5)
            if (portal != null && entity.renderBoundingBox.intersects(portal.remoteBoundingBox)) {
                // Disable stencil test for entities inside the portal.
                // Only affects the correct side, ones on the wrong side will not be rendered in the first place.
                GL11.glDisable(GL11.GL_STENCIL_TEST)
                stencilStack.add(true)
            } else {
                stencilStack.add(false)
            }
            return true
        }

        fun afterRender(entity: Entity) {
            if (entity is Portal) return
            if (clippingStack.removeAt(clippingStack.size - 1)) {
                GL11.glDisable(GL11.GL_CLIP_PLANE4)
            }

            if (ViewRenderPlan.CURRENT?.parentPortal == null) return

            GL11.glEnable(GL11.GL_CLIP_PLANE5)
            if (stencilStack.removeAt(stencilStack.size - 1)) {
                GL11.glEnable(GL11.GL_STENCIL_TEST)
            }
        }

        fun createCamera() = ViewRenderPlan.CURRENT?.camera
        /*
        fun createCamera(): Frustum? {
            if (portalStack.isEmpty()) return null
            val instance = portalStack.last()
            if (instance.isPlayerInPortal) return null // lots of edge cases here for little gain, don't even try (for now)
            val pos = Minecraft.getMinecraft().renderViewEntity!!.getPositionEyes(1.0f)
            return PortalCamera(instance.portal, pos, Frustum())
        }
         */
    }

    open class Instance<out T : AbstractPortalEntity>(
            val entity: T,
            val x: Double,
            val y: Double,
            val z: Double,
            partialTicks: Float
    ) {
        companion object {
            // FIXME get rid of Instance and put this in AbstractRenderPortal
            private val shader = ShaderManager(mc.resourceManager, "betterportals:render_portal")
        }
        val portal = entity
        val player: EntityPlayerSP = mc.player
        val isPlayerInPortal = portal.localBoundingBox.intersects(player.entityBoundingBox)
                && portal.localBlocks.any { AxisAlignedBB(it).intersects(player.entityBoundingBox) }

        val portalRotation = portal.localRotation
        val portalFacing = portal.localFacing
        /**
         * Side of the portal on which the player's eyes are.
         */
        val viewFacing = portalFacing.axis.toFacing(player.getPositionEyes(partialTicks) - entity.pos)

        open fun render() {
            GlStateManager.disableAlpha() // ._. someone forgot to disable this, thanks (happens if chat GUI is opened)

            if (entity.isDead) {
                return
            }

            val parentPortal = ViewRenderPlan.CURRENT?.parentPortal
            if (parentPortal?.world == portal.view?.camera?.world && parentPortal?.remotePosition == entity.localPosition) {
                // Skip rendering of portal if it's the remote to the portal we're currently in
                return
            }

            val occlusionQuery = ViewRenderManager.INSTANCE.getOcclusionQuery(entity)
            occlusionQuery.begin()

            val framebuffer = ViewRenderPlan.CURRENT?.framebuffers?.get(entity)
            // TODO: the OF shaders check is only here because it's the easy fix to us using shaders to draw the portal
            //       (while OF is using shaders which doesn't fly). once view rendering works with OF shaders, this
            //       check should be removed and an alternative approach to rendering the portal face should be used.
            if (framebuffer == null || Optifine?.shadersActive == true) {
                renderPortalInactive()
            } else {
                shader.addSamplerTexture("sampler", framebuffer.framebufferTexture)
                shader.addSamplerTexture("depthSampler", framebuffer.depthTexture)
                shader.getShaderUniformOrDefault("screenSize")
                        .set(framebuffer.framebufferWidth.toFloat(), framebuffer.framebufferHeight.toFloat())
                shader.useShader()
                renderPortalFromInside()
                shader.endShader()
            }

            occlusionQuery.end()

            /*
            GlStateManager.pushMatrix()
            GlStateManager.pushAttrib()

            GlStateManager.disableTexture2D()

            // Step one, draw portal face onto stencil buffer where visible (and prepare occlusion culling)
            glMask(false, false, false, false, false, 0xff)
            GL11.glEnable(GL11.GL_STENCIL_TEST)
            GL11.glClearStencil(0x00)
            GL11.glClear(GL11.GL_STENCIL_BUFFER_BIT)
            GL11.glStencilFunc(GL11.GL_ALWAYS, 0xff, 0xff)
            GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_REPLACE)
            GL15.glBeginQuery(GL15.GL_SAMPLES_PASSED, state.allocOcclusionQuery())
            renderPortalFromInside()
            GL15.glEndQuery(GL15.GL_SAMPLES_PASSED)
            GL11.glStencilFunc(GL11.GL_EQUAL, 0xff, 0xff)
            GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP)

            // Occlusion culling
            state.update()
            if (state.occluded) {
                GlStateManager.popAttrib()
                GlStateManager.popMatrix()
                return
            }

            val debugBoundingBox = mc.renderManager.isDebugBoundingBox

            view.withView {
                mc.renderManager.isDebugBoundingBox = debugBoundingBox

                // Step two, reset depth buffer (and color sky) where stencil buffer is marked
                glMask(true, true, true, true, true, 0x00)
                GlStateManager.depthFunc(GL11.GL_ALWAYS)
                GL11.glDepthRange(1.0, 1.0) // any depth is 1

                GlStateManager.disableFog()
                GlStateManager.disableLighting()
                mc.entityRenderer.disableLightmap()
                mc.entityRenderer.updateFogColor(mc.renderPartialTicks)
                with(GlStateManager.clearState.color) { GlStateManager.color(red, green, blue) }

                glMask(true, true, true, false, true, 0x00)
                renderFullScreen()
                GL11.glDepthRange(0.0, 1.0)
                GlStateManager.depthFunc(GL11.GL_LESS)

                // Step three, draw portal content where stencil buffer is marked
                glMask(true, true, true, true, true, 0x00)
                val planePos = Vec3d(x, y, z) + viewFacing.directionVec.to3d().scale(0.5)
                glClipPlane(GL11.GL_CLIP_PLANE5, viewFacing.directionVec.to3d().scale(-1.0), planePos)
                GL11.glEnable(GL11.GL_CLIP_PLANE5) // FIXME don't hard-code clipping plane id

                GlStateManager.enableTexture2D()

                val viewManager =  view.manager as ClientViewManagerImpl
                viewManager.yawOffset = (portal.remoteRotation - portal.localRotation).degrees.toFloat()
                val dist = Vec3d(x, y, z).lengthVector().toFloat()
                when (GlStateManager.FogMode.values().find { it.capabilityId == GlStateManager.fogState.mode }) {
                    GlStateManager.FogMode.LINEAR -> viewManager.fogOffset = dist
                    // TODO
                    else -> viewManager.fogOffset = 0f
                }

                entity.world.profiler.startSection("renderView" + view.id)

                portalStack.add(this)
                mc.entityRenderer.renderWorld(partialTicks, System.nanoTime())
                portalStack.removeAt(portalStack.size - 1)

                entity.world.profiler.endSection()
            }

            GlStateManager.disableTexture2D()

            // Recover from that
            ForgeHooksClient.setRenderPass(0)

            GlStateManager.popAttrib()
            GlStateManager.popMatrix()

            // Step four, apply (fake) fog of current dimension to portal
            // Note that this fog isn't real fog (i.e. it is constant regardless of depth) because of limitations of
            // the GL fixed function pipeline
            with(GlStateManager.fogState) {
                val dist = Vec3d(x, y, z).lengthVector()
                // See https://www.khronos.org/registry/OpenGL-Refpages/gl2.1/xhtml/glFog.xml for how to calculate f
                val f = MathHelper.clamp(when (GlStateManager.FogMode.values().find { it.capabilityId == mode }) {
                    GlStateManager.FogMode.LINEAR -> (end - dist) / (end - start)
                    GlStateManager.FogMode.EXP -> Math.exp(-density * dist)
                    GlStateManager.FogMode.EXP2  -> Math.exp(-density * dist * density * dist)
                    else -> 1.0
                }.toFloat(), 0f, 1f)
                GlStateManager.pushAttrib()
                glMask(true, true, true, false, false, 0x00)
                GL11.glEnable(GL11.GL_STENCIL_TEST)
                GL11.glStencilFunc(GL11.GL_EQUAL, 0xff, 0xff)
                GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP)
                GlStateManager.disableTexture2D()
                GlStateManager.disableFog()
                GlStateManager.disableLighting()
                mc.entityRenderer.disableLightmap()
                with(GlStateManager.clearState.color) { GlStateManager.color(red, green, blue, 1 - f) }
                GlStateManager.enableBlend()
                GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA)
                renderFullScreen()
                GlStateManager.popAttrib()
                // for some reason this doesn't reset properly, TODO find out why popAttrib doesn't work in this case
                with(GlStateManager.colorState) { GL11.glColor4f(red, green, blue, alpha) }
            }

            // Step five, reset depth buffer values of portal face to the portal face instead of exclusively behind it
            glMask(false, false, false, false, true, 0x00)
            GlStateManager.pushAttrib()
            GL11.glEnable(GL11.GL_STENCIL_TEST)
            GL11.glStencilFunc(GL11.GL_EQUAL, 0xff, 0xff)
            GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP)
            renderFullScreen()
            GlStateManager.popAttrib()
            glMask(true, true, true, true, true, 0x00)
            */
        }

        private fun renderPortalInactive() {
            GlStateManager.color(0f, 0f, 0f)
            renderPortalFromInside()
            GlStateManager.color(1f, 1f, 1f)
        }

        private fun renderPortalFromInside() {
            val tessellator = Tessellator.getInstance()
            val offset = Vec3d(x - 0.5, y - 0.5, z - 0.5)

            with(tessellator.buffer) {
                begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION)

                val blocks = portal.relativeBlocks.map { it.rotate(portalRotation) }
                blocks.forEach { pos ->
                    setTranslation(offset.x + pos.x, offset.y + pos.y, offset.z + pos.z)
                    EnumFacing.VALUES.forEach facing@ { facing ->
                        if (blocks.contains(pos.offset(facing))) return@facing
                        if (facing == viewFacing) return@facing

                        renderPartialPortalFace(this, facing)
                    }
                }

                setTranslation(0.0, 0.0, 0.0)
            }

            GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL)
            GL11.glPolygonOffset(-1f, -1f)
            tessellator.draw()
            GL11.glPolygonOffset(0f, 0f)
            GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL)
        }

        protected open fun renderPartialPortalFace(bufferBuilder: BufferBuilder, facing: EnumFacing) {
            // Drawing a cube has never been easier
            val xF = facing.frontOffsetX * 0.5
            val yF = facing.frontOffsetY * 0.5
            val zF = facing.frontOffsetZ * 0.5
            var rotFacing = if (facing.axis == EnumFacing.Axis.Y) EnumFacing.NORTH else EnumFacing.UP
            (0..3).map { _ ->
                val nextRotFacing = rotFacing.rotateAround(facing.axis).let {
                    if (facing.axisDirection == EnumFacing.AxisDirection.POSITIVE) it else it.opposite
                }
                bufferBuilder.pos(
                        xF + rotFacing.frontOffsetX * 0.5 + nextRotFacing.frontOffsetX * 0.5 + 0.5,
                        (yF + rotFacing.frontOffsetY * 0.5 + nextRotFacing.frontOffsetY * 0.5 + 0.5),
                        zF + rotFacing.frontOffsetZ * 0.5 + nextRotFacing.frontOffsetZ * 0.5 + 0.5
                ).endVertex()
                rotFacing = nextRotFacing
            }
        }
    }

    abstract fun createInstance(entity: T, x: Double, y: Double, z: Double, partialTicks: Float): Instance<T>

    override fun doRender(entity: T, x: Double, y: Double, z: Double, entityYaw: Float, partialTicks: Float) {
        createInstance(entity, x, y, z, partialTicks).render()
    }

    override fun doRenderShadowAndFire(entityIn: Entity, x: Double, y: Double, z: Double, yaw: Float, partialTicks: Float) {}

    override fun getEntityTexture(entity: T): ResourceLocation? = null
}
