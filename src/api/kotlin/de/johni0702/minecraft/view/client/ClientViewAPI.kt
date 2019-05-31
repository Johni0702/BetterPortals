package de.johni0702.minecraft.view.client

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
     * Returns the client-side view manager responsible for the given instance of the Minecraft client.
     */
    fun getViewManager(minecraft: Minecraft): ClientViewManager
}