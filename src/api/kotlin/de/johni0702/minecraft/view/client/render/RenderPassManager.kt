package de.johni0702.minecraft.view.client.render

/**
 * Manages the way in which multiple views will be rendered in order to produce one final frame.
 *
 * For each frame, it will first build a tree of [RenderPass]es, then render each one to its own framebuffer and
 * finally render the result of the root [RenderPass] to Minecraft's framebuffer.
 */
interface RenderPassManager {
    /**
     * The root node of the [RenderPass] tree.
     * This is the last pass to be drawn and its result will be visible on the screen.
     *
     * May be `null` when not rendering.
     */
    val root: RenderPass?

    /**
     * The currently active [RenderPass].
     *
     * May be `null` when not rendering.
     */
    val current: RenderPass?
}