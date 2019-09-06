package de.johni0702.minecraft.betterportals.impl.transition.net

import de.johni0702.minecraft.betterportals.impl.IMessage
import de.johni0702.minecraft.betterportals.impl.IMessageHandler
import de.johni0702.minecraft.betterportals.impl.MessageContext
import de.johni0702.minecraft.betterportals.impl.NetworkDirection
import de.johni0702.minecraft.betterportals.impl.sync
import de.johni0702.minecraft.betterportals.impl.transition.client.renderer.TransferToDimensionRenderer
import io.netty.buffer.ByteBuf

/**
 * Sent to the client when [net.minecraft.server.management.PlayerList.transferPlayerToDimension] is called
 * shortly before the main world is changed. It is up to the client to notify the server
 * that the transition is complete and that the current/soon-to-be-old main view is no longer
 * needed by sending a [TransferToDimensionDone] message.
 */
internal class TransferToDimension(var id: Int = 0) : IMessage {
    override val direction = NetworkDirection.TO_CLIENT

    override fun fromBytes(buf: ByteBuf) {
        id = buf.readInt()
    }

    override fun toBytes(buf: ByteBuf) {
        buf.writeInt(id)
    }

    internal class Handler : IMessageHandler<TransferToDimension> {
        override fun new(): TransferToDimension = TransferToDimension()

        override fun handle(message: TransferToDimension, ctx: MessageContext) {
            ctx.sync {
                val whenDone = {
                    Net.INSTANCE.sendToServer(TransferToDimensionDone(message.id))
                }
                TransferToDimensionRenderer(whenDone)
            }
        }
    }
}