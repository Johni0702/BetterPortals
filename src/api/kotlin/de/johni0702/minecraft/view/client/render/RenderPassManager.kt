package de.johni0702.minecraft.view.client.render

import de.johni0702.minecraft.view.client.ClientViewAPI
import de.johni0702.minecraft.view.client.worldsManager
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.WorldClient
import de.johni0702.minecraft.view.common.fabricEvent
import net.minecraftforge.fml.common.eventhandler.Event

/**
 * Manages the way in which multiple views will be rendered in order to produce one final frame.
 *
 * For each frame, it will first build a tree of [RenderPass]es, then render each one to its own framebuffer and
 * finally render the result of the root [RenderPass] to Minecraft's framebuffer.
 *
 * To get the current instance, call [ClientViewAPI.getRenderPassManager].
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

    /**
     * The [RenderPass] of the previous frame.
     * For reference only, should not be modified.
     */
    val previous: RenderPass?
}

val Minecraft.renderPassManager get() = ClientViewAPI.instance.getRenderPassManager(this)

/**
 * Emitted to determine the [root][RenderPassManager.root] node of the render pass tree.
 * The default root node mimics vanilla.
 */
class DetermineRootPassEvent(
        val manager: RenderPassManager,
        val partialTicks: Float,
        world: WorldClient,
        var camera: Camera
) : Event() {
    companion object { @Suppress("unused") @JvmField val EVENT = fabricEvent<DetermineRootPassEvent>() }

    var world = world
        set(value) {
            val worldsManager = Minecraft.getMinecraft().worldsManager
            require(world in worldsManager!!.worlds) { "Unknown world $world" }
            field = value
        }
}

/**
 * Emitted to populate the render pass tree, i.e. add more passes where required.
 *
 * The event may be emitted multiple times for each tree in order to allow all participants to inspect the full tree.
 * If you've substantially changed the tree, make sure to set [changed] to `true` to give others a chance to
 * react to your changes.
 */
class PopulateTreeEvent(
        val partialTicks: Float,

        /**
         * The root node of the tree.
         */
        val root: RenderPass,

        /**
         * When set to true, the event will be re-emitted to give everyone a fair chance of dealing with the updated
         * tree.
         */
        var changed: Boolean
) : Event()
{ companion object { @Suppress("unused") @JvmField val EVENT = fabricEvent<PopulateTreeEvent>() } }
