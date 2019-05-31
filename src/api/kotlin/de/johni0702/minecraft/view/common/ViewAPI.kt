package de.johni0702.minecraft.view.common

import de.johni0702.minecraft.view.client.ClientViewAPI
import de.johni0702.minecraft.view.server.ServerViewAPI
import net.minecraftforge.fml.common.Loader

/**
 * Entry point into the view API. See [ClientViewAPI] and [ServerViewAPI]
 */
interface ViewAPI {
    companion object {
        @JvmStatic
        val instance by lazy { Loader.instance().indexedModList["betterportals"]!!.mod as ViewAPI }
    }

    /**
     * Client-side entry point into the view API.
     * Not to be accessed from the server.
     */
    val client: ClientViewAPI

    /**
     * Server-side entry point into the view API.
     * Not to be accessed from the client.
     */
    val server: ServerViewAPI
}