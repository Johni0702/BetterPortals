package de.johni0702.minecraft.view.impl.client

import de.johni0702.minecraft.view.impl.MOD_ID
import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.util.ReferenceCountUtil
import net.minecraft.network.play.server.SPacketCustomPayload

internal class TransactionNettyHandler : ChannelInboundHandlerAdapter() {
    private var inTransaction = 0
    private val queue = mutableListOf<Any>()

    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        if (msg is SPacketCustomPayload) {
            if (msg.channelName == "$MOD_ID|TS") {
                inTransaction++
                return
            }
            if (msg.channelName == "$MOD_ID|TE") {
                if (inTransaction <= 1) {
                    queue.forEach {
                        ReferenceCountUtil.release(it)
                        super.channelRead(ctx, it)
                    }
                    queue.clear()
                }
                inTransaction--
                return
            }
        }
        if (inTransaction > 0) {
            ReferenceCountUtil.retain(msg)
            queue.add(msg)
        } else {
            super.channelRead(ctx, msg)
        }
    }

    companion object {
        @JvmStatic
        fun inject(channel: Channel) {
            channel.pipeline().addBefore("packet_handler", "transaction_handler", TransactionNettyHandler())
        }
    }
}