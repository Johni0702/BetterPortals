package de.johni0702.minecraft.view.client.render

import de.johni0702.minecraft.view.common.fabricEvent
import net.minecraft.client.multiplayer.WorldClient
import net.minecraft.client.shader.Framebuffer
import de.johni0702.minecraft.view.common.Cancelable
import de.johni0702.minecraft.view.common.Event

/**
 * Contains settings and dependencies for rendering a single view.
 *
 * Mods may attach arbitrary details to any render pass which they can later use during rendering and/or to interact
 * with other mods.
 */
interface RenderPass {
    /**
     * The associated manager instance.
     */
    val manager: RenderPassManager

    /**
     * The parent render pass. `null` for the root pass.
     */
    val parent: RenderPass?

    /**
     * The world which will be rendered.
     */
    val world: WorldClient

    /**
     * The camera from which the view will be rendered.
     */
    val camera: Camera

    /**
     * The framebuffer which this pass is rendered on.
     * May be null if rendering hasn't started.
     */
    val framebuffer: Framebuffer?

    /**
     * List of render passes which need to be rendered before this one.
     */
    val children: List<RenderPass>

    /**
     * Create a new render pass, add it as a dependency to this pass and then return it for further configuration.
     *
     * Optionally, a render pass from a previous frame can be passed which allows tracking of certain information
     * (e.g. occlusion queries) across multiple frames.
     */
    fun addChild(world: WorldClient, camera: Camera, previousFrame: RenderPass?): RenderPass

    /**
     * Set detail of given type.
     */
    operator fun <T> set(type: Class<T>, detail: T?)

    /**
     * Return the detail of given type or `null` if not set.
     */
    operator fun <T> get(type: Class<T>): T?
}

// Note: The following are reified versions of methods above for ergonomic usage from kotlin.

/**
 * Set detail of given type.
 */
inline fun <reified T> RenderPass.get(): T? = get(T::class.java)

/**
 * Return the detail of given type or `null` if not set.
 */
inline fun <reified T> RenderPass.set(detail: T?) = set(T::class.java, detail)



abstract class RenderPassEvent(
        val partialTicks: Float,
        val renderPass: RenderPass
) : Event() {
    /**
     * Emitted before a [RenderPass] or any of its children are run, early enough to cancel all of it.
     */
    @Cancelable
    class Prepare(partialTicks: Float, renderPass: RenderPass) : RenderPassEvent(partialTicks, renderPass)
    { companion object { @Suppress("unused") @JvmField val EVENT = fabricEvent<Prepare>() } }

    /**
     * Emitted before a [RenderPass] is run, after all children have been run but still early enough to cancel it.
     */
    @Cancelable
    class Before(partialTicks: Float, renderPass: RenderPass) : RenderPassEvent(partialTicks, renderPass)
    { companion object { @Suppress("unused") @JvmField val EVENT = fabricEvent<Before>() } }

    /**
     * Emitted right before the view of a [RenderPass] is drawn.
     * All its children have already been drawn (except those that were cancelled).
     * The framebuffer has been set up and bound but rendering has not yet begun.
     */
    class Start(partialTicks: Float, renderPass: RenderPass) : RenderPassEvent(partialTicks, renderPass)
    { companion object { @Suppress("unused") @JvmField val EVENT = fabricEvent<Start>() } }

    /**
     * Emitted right after the view of a [RenderPass] has been drawn.
     * The framebuffer has been set up, bound (and still is) and drawn onto.
     */
    class End(partialTicks: Float, renderPass: RenderPass) : RenderPassEvent(partialTicks, renderPass)
    { companion object { @Suppress("unused") @JvmField val EVENT = fabricEvent<End>() } }

    /**
     * Emitted after the view of a [RenderPass] has been drawn.
     * The framebuffer has been set up, bound, drawn onto, finally unbound and is now ready to use for the parent view.
     * If any post processing of the framebuffer or general cleanup needs to happen, now is the time for that.
     */
    class After(partialTicks: Float, renderPass: RenderPass) : RenderPassEvent(partialTicks, renderPass)
    { companion object { @Suppress("unused") @JvmField val EVENT = fabricEvent<After>() } }
}
