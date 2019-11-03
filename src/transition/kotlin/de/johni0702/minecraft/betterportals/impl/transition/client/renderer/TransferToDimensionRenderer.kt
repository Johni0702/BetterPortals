package de.johni0702.minecraft.betterportals.impl.transition.client.renderer

import de.johni0702.minecraft.betterportals.common.*
import de.johni0702.minecraft.view.client.render.*
import de.johni0702.minecraft.view.client.worldsManager
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.WorldClient
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.client.shader.ShaderManager
import org.lwjgl.opengl.GL11
import java.time.Duration

//#if FABRIC>=1
//$$ import de.johni0702.minecraft.view.common.BPCallback
//$$ import net.fabricmc.fabric.api.event.client.ClientTickCallback
//#else
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
//#endif

internal class TransferToDimensionRenderer(
        private val whenDone: () -> Unit,
        private val duration: Duration = defaultDuration()
) {
    companion object {
        lateinit var defaultDuration: () -> Duration
    }
    private val mc = Minecraft.getMinecraft()

    private val shader = ShaderManager(mc.resourceManager, "betterportals:dimension_transition")
    private val eventHandler = EventHandler()

    private val fromWorld = mc.world
    private val fromInitialPitch = mc.player.rotationPitch
    private val fromInitialYaw = mc.player.rotationYaw
    private val fromInitialPos = mc.player.tickPos
    private var toInitialized = false
    private var toWorld: WorldClient = fromWorld
    private var toInitialYaw = fromInitialYaw
    private var toInitialPos = fromInitialPos
    private val cameraYawOffset
        get() = toInitialYaw - fromInitialYaw
    private val cameraPosOffset
        get() = Mat4d.add(fromInitialPos.toJavaX()) * Mat4d.rotYaw(cameraYawOffset) * Mat4d.sub(toInitialPos.toJavaX())
    private var ticksPassed = 0

    private fun getProgress(partialTicks: Float) = ((ticksPassed + partialTicks) * 50 / duration.toMillis()).coerceIn(0f, 1f)

    init {
        eventHandler.registered = true
    }

    private fun addOldViewToTree(event: PopulateTreeEvent) {
        val root = event.root
        if (root.children.any { it.get<TransferToDimensionRenderer>() == this }) {
            return
        }
        val prevRoot = root.manager.previous
        val prevChild = prevRoot?.children?.find { it.get<TransferToDimensionRenderer>() == this }

        if (!toInitialized) {
            with(mc.player) {
                // Copy current pitch onto new camera to prevent sudden camera jumps when switching views
                rotationPitch = fromInitialPitch
                prevRotationPitch = fromInitialPitch

                // Initialize to-values
                toWorld = mc.world
                toInitialYaw = rotationYaw
                toInitialPos = tickPos
                toInitialized = true
            }
        }

        // TODO this isn't quite right once we use a portal in the new view while the transition is still active
        val camera = root.camera.transformed(cameraPosOffset)
        root.addChild(fromWorld, camera, prevChild).set(this)
        event.changed = true
    }

    private fun renderTransition(rootPass: RenderPass, partialTicks: Float) {
        val childPass = rootPass.children.find { it.get<TransferToDimensionRenderer>() == this } ?: return
        val framebuffer = childPass.framebuffer ?: return

        shader.addSamplerTexture("sampler", framebuffer)
        shader.getShaderUniformOrDefault("screenSize")
                .set(framebuffer.framebufferWidth.toFloat(), framebuffer.framebufferHeight.toFloat())
        shader.getShaderUniformOrDefault("progress").set(getProgress(partialTicks))
        shader.useShader()

        val tessellator = Tessellator.getInstance()
        tessellator.buffer.apply {
            begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION)
            pos( 1.0,  1.0,  1.0).endVertex()
            pos(-1.0,  1.0,  1.0).endVertex()
            pos(-1.0, -1.0,  1.0).endVertex()
            pos( 1.0, -1.0,  1.0).endVertex()
        }
        tessellator.draw()

        shader.endShader()
    }

    //#if FABRIC>=1
    //$$ private object CallbackDispatcher {
    //$$     val handlers = mutableListOf<EventHandler>()
    //$$
    //$$     init {
    //$$         ClientTickCallback.EVENT.register(ClientTickCallback {
    //$$             handlers.forEach { it.clientTick() }
    //$$         })
    //$$         PopulateTreeEvent.EVENT.register(BPCallback {  event ->
    //$$             handlers.forEach { it.populateTree(event) }
    //$$         })
    //$$         // TODO render
    //$$     }
    //$$ }
    //$$ private inner class EventHandler {
    //$$     var registered = false
    //$$         set(value) {
    //$$             if (field == value) return
    //$$             if (value) {
    //$$                 CallbackDispatcher.handlers.add(this)
    //$$             } else {
    //$$                 CallbackDispatcher.handlers.remove(this)
    //$$             }
    //$$             field = value
    //$$         }
    //#else
    private inner class EventHandler {
        var registered by MinecraftForge.EVENT_BUS

        @SubscribeEvent
        fun preClientTick(event: TickEvent.ClientTickEvent) {
            if (event.phase != TickEvent.Phase.START) return
            clientTick()
        }

        @SubscribeEvent
        fun onPopulateTreeEvent(event: PopulateTreeEvent) {
            populateTree(event)
        }

        @SubscribeEvent(priority = EventPriority.LOW)
        fun onRenderWorldLast(event: RenderWorldLastEvent) {
            render(event.partialTicks)
        }
    //#endif

        fun clientTick() {
            ticksPassed++

            val manager = Minecraft.getMinecraft().worldsManager ?: return
            if (fromWorld !in manager.worlds || toWorld !in manager.worlds || getProgress(0f) >= 1) {
                registered = false
                shader.deleteShader()
                whenDone()
            }
        }

        fun populateTree(event: PopulateTreeEvent) {
            val manager = Minecraft.getMinecraft().worldsManager ?: return
            if (fromWorld !in manager.worlds || toWorld !in manager.worlds) return
            addOldViewToTree(event)
        }

        fun render(partialTicks: Float) {
            val manager = Minecraft.getMinecraft().renderPassManager
            val rootPass = manager.root ?: return
            if (manager.current == rootPass) {
                renderTransition(rootPass, partialTicks)
            }
        }
    }
}