package de.johni0702.minecraft.betterportals.net

import de.johni0702.minecraft.betterportals.BetterPortalsMod.Companion.viewManager
import de.johni0702.minecraft.betterportals.LOGGER
import de.johni0702.minecraft.betterportals.client.renderer.TransferToDimensionRenderer
import de.johni0702.minecraft.betterportals.common.sync
import io.netty.buffer.ByteBuf
import net.minecraftforge.fml.common.network.simpleimpl.IMessage
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext

/**
 * Sent to the client when [net.minecraft.server.management.PlayerList.transferPlayerToDimension] is called.
 * The [toId] is the soon-to-be main view of the new dimension. It is up to the client to notify the server
 * that the transition is complete and that the current/soon-to-be-old main view with id [fromId] is no longer needed
 * by sending a [TransferToDimensionDone] message.
 */
internal class TransferToDimension(
        var fromId: Int = 0,
        var toId: Int = 0
) : IMessage {

    override fun fromBytes(buf: ByteBuf) {
        fromId = buf.readInt()
        toId = buf.readInt()
    }

    override fun toBytes(buf: ByteBuf) {
        buf.writeInt(fromId)
        buf.writeInt(toId)
    }

    internal class Handler : IMessageHandler<TransferToDimension, IMessage> {
        override fun onMessage(message: TransferToDimension, ctx: MessageContext): IMessage? {
            ctx.sync {
                val whenDone = {
                    Net.INSTANCE.sendToServer(TransferToDimensionDone(message.fromId))
                }
                val fromView = viewManager.views.find { it.id == message.fromId }
                if (fromView == null) {
                    LOGGER.warn("Got TransferToDimension message for non-existent source view with id {}", message.fromId)
                    return@sync whenDone()
                }
                val toView = viewManager.views.find { it.id == message.toId }
                if (toView == null) {
                    LOGGER.warn("Got TransferToDimension message for non-existent destination view with id {}", message.toId)
                    return@sync whenDone()
                }
                TransferToDimensionRenderer(fromView, toView, whenDone)
            }
            return null
        }
    }
}