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
     * Returns the server-side view manager responsible for the given connection.
     */
    fun getViewManager(connection: NetHandlerPlayServer): ServerViewManager

    /**
     * Returns the server-side view manager responsible for the given player.
     */
    fun getViewManager(player: EntityPlayerMP) = getViewManager(player.connection)

    /**
     * Returns the server-side view for which the given player is the camera.
     * Can return null e.g. for [net.minecraftforge.common.util.FakePlayer].
     */
    fun getView(player: EntityPlayerMP): ServerView?
}