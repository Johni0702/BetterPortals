package de.johni0702.minecraft.betterportals.net

import de.johni0702.minecraft.betterportals.BetterPortalsMod
import de.johni0702.minecraft.betterportals.LOGGER
import de.johni0702.minecraft.betterportals.common.sync
import de.johni0702.minecraft.betterportals.server.view.viewManager
import io.netty.buffer.ByteBuf
import net.minecraftforge.fml.common.network.simpleimpl.IMessage
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext

/**
 * Sent from the client when a dimension change initiated by a [TransferToDimension] message has been completed.
 * The view with Id [viewId] will subsequently be destroyed unless it is used elsewhere.
 */
internal class TransferToDimensionDone(
        var viewId: Int = 0
) : IMessage {

    override fun fromBytes(buf: ByteBuf) {
        viewId = buf.readInt()
    }

    override fun toBytes(buf: ByteBuf) {
        buf.writeInt(viewId)
    }

    internal class Handler : IMessageHandler<TransferToDimensionDone, IMessage> {
        override fun onMessage(message: TransferToDimensionDone, ctx: MessageContext): IMessage? {
            ctx.sync {
                val view = ctx.serverHandler.player.viewManager.views.find { it.id == message.viewId }
                if (view == null) {
                    LOGGER.warn("Got TransferToDimensionDone message for unknown source view with id {}", message.viewId)
                    return@sync
                }
                view.release()
            }
            return null
        }
    }
}