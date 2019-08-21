package de.johni0702.minecraft.view.client

import de.johni0702.minecraft.view.client.render.RenderPassManager
import de.johni0702.minecraft.view.common.ViewAPI
import net.minecraft.client.Minecraft

/**
 * Client-side entry point into the view API.
 */
interface ClientViewAPI {
    companion object {
        @JvmStatic
        val instance get() = ViewAPI.instance.client
    }

    /**
     * Returns the client-side worlds manager responsible for the given instance of the Minecraft client.
     * Returns `null` when no worlds manager is currently present (e.g. while still in the main menu or not yet fully
     * connected).
     */
    fun getWorldsManager(minecraft: Minecraft): ClientWorldsManager?

    /**
     * Return the [RenderPassManager] responsible for the given instance of the Minecraft client.
     */
    fun getRenderPassManager(minecraft: Minecraft): RenderPassManager
}