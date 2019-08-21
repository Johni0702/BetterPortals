package de.johni0702.minecraft.view.server

import de.johni0702.minecraft.view.common.ViewAPI
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.network.NetHandlerPlayServer

/**
 * Server-side entry point into the view API.
 */
interface ServerViewAPI {
    companion object {
        @JvmStatic
        val instance get() = ViewAPI.instance.server
    }

    /**
     * Returns the server-side world manager responsible for the given connection.
     */
    fun getWorldsManager(connection: NetHandlerPlayServer): ServerWorldsManager

    /**
     * Returns the server-side world manager responsible for the given player.
     */
    fun getWorldsManager(player: EntityPlayerMP) = getWorldsManager(player.connection)
}