package de.johni0702.minecraft.betterportals.client.view

import de.johni0702.minecraft.betterportals.*
import io.netty.channel.ChannelHandlerContext
import io.netty.util.concurrent.Future
import io.netty.util.concurrent.GenericFutureListener
import net.minecraft.network.EnumPacketDirection
import net.minecraft.network.NetworkManager
import net.minecraft.network.Packet
import net.minecraft.network.play.client.CPacketKeepAlive
import net.minecraft.network.play.server.SPacketChat

/**
 * NetworkManager which discards all packets instead of sending them.
 * It also discards incoming packets which shouldn't be handled for views (e.g. chat).
 */
internal class ViewNetworkManager : NetworkManager(EnumPacketDirection.CLIENTBOUND) {

    override fun sendPacket(packetIn: Packet<*>) {
        val viewManager = BetterPortalsMod.viewManagerImpl
        // FIXME does sending keep alive responses actually make sense here?
        if (packetIn is CPacketKeepAlive || viewManager.activeView.isMainView) {
            // Send packet via main connection
            viewManager.serverMainView.netManager?.sendPacket(packetIn)
        } else {
            LOGGER.warn("Dropping packet {}", packetIn)
        }
    }

    override fun sendPacket(packetIn: Packet<*>, listener: GenericFutureListener<out Future<in Void>>, vararg listeners: GenericFutureListener<out Future<in Void>>) {
    }

    override fun channelRead0(ctx: ChannelHandlerContext, packet: Packet<*>) {
        when (packet) {
            is SPacketChat -> return
            else -> super.channelRead0(ctx, packet)
        }
    }

    @Throws(Exception::class)
    override fun exceptionCaught(ctx: ChannelHandlerContext?, t: Throwable) {
        LOGGER.error("Error handing view data for view ${BetterPortalsMod.viewManager.activeView}:", t)
    }
}
