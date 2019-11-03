package de.johni0702.minecraft.view.impl.compat

import de.johni0702.minecraft.betterportals.common.hasClass
import de.johni0702.minecraft.view.client.render.RenderPassEvent
import de.johni0702.minecraft.view.impl.LOGGER
import net.minecraft.client.renderer.chunk.RenderChunk
import net.optifine.render.VboRegion

//#if FABRIC>=1
//$$ import de.johni0702.minecraft.view.common.register
//#else
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
//#endif

interface OFRenderChunk {
    val renderChunkNeighbours: Array<RenderChunk>
    var renderChunkNeighboursUpdated: Boolean
}

interface OFVertexBuffer {
    fun setVboRegion(region: VboRegion)
}

interface OFViewFrustum {
    fun refVboRegion(chunk: RenderChunk)
    fun unrefVboRegion(chunk: RenderChunk)
}

internal class OptifineReflection {
    private val Shaders = Class.forName("net.optifine.shaders.Shaders")!!
    private val Shaders_shaderPackLoaded = Shaders.getDeclaredField("shaderPackLoaded")!!

    var shadersActive: Boolean
        get() = Shaders_shaderPackLoaded[null] as Boolean
        set(value) { Shaders_shaderPackLoaded[null] = value }
}

internal val Optifine = if (hasClass("optifine.OptiFineForgeTweaker")) {
    OptifineReflection()
} else {
    null
}

internal fun registerOptifineCompat() {
    val optifine = Optifine ?: return

    LOGGER.info("Optifine detected. See-through portals will be disabled while Shaders are active.")

    fun preRenderView(event: RenderPassEvent.Prepare) {
        // TODO: OF might allow portals to be rendered with shaders if the shader pack uses the same shaders for
        //       each dimension. should we enable that, we also need to stop using shaders to draw portal surfaces
        if (optifine.shadersActive && event.renderPass != event.renderPass.manager.root) {
            event.isCanceled = true
        }
    }

    //#if FABRIC>=1
    //$$ RenderPassEvent.Prepare.EVENT.register { preRenderView(it) }
    //#else
    MinecraftForge.EVENT_BUS.register(object {
        @SubscribeEvent(priority = EventPriority.LOW)
        fun preRenderView(event: RenderPassEvent.Prepare) {
            preRenderView(event)
        }
    })
    //#endif
}
