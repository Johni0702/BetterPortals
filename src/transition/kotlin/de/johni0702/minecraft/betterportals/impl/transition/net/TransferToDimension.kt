package de.johni0702.minecraft.betterportals.impl.transition.net

import de.johni0702.minecraft.betterportals.impl.transition.client.renderer.TransferToDimensionRenderer
import de.johni0702.minecraft.betterportals.impl.transition.common.sync
import io.netty.buffer.ByteBuf
import net.minecraftforge.fml.common.network.simpleimpl.IMessage
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext

/**
 * Sent to the client when [net.minecraft.server.management.PlayerList.transferPlayerToDimension] is called
 * shortly before the main world is changed. It is up to the client to notify the server
 * that the transition is complete and that the current/soon-to-be-old main view is no longer
 * needed by sending a [TransferToDimensionDone] message.
 */
internal class TransferToDimension : IMessage {

    override fun fromBytes(buf: ByteBuf) {
    }

    override fun toBytes(buf: ByteBuf) {
    }

    internal class Handler : IMessageHandler<TransferToDimension, IMessage> {
        override fun onMessage(message: TransferToDimension, ctx: MessageContext): IMessage? {
            ctx.sync {
                val whenDone = {
                    Net.INSTANCE.sendToServer(TransferToDimensionDone())
                }
                TransferToDimensionRenderer(whenDone)
            }
            return null
        }
    }
}