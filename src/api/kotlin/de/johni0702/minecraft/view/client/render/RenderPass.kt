package de.johni0702.minecraft.view.client.render

import de.johni0702.minecraft.view.client.ClientView
import net.minecraft.client.shader.Framebuffer

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
     * The view which will be rendered.
     */
    val view: ClientView

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
     */
    fun addChild(view: ClientView, camera: Camera): RenderPass

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
