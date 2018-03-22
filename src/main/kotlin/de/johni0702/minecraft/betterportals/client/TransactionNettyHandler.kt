package de.johni0702.minecraft.betterportals.client

import de.johni0702.minecraft.betterportals.MOD_ID
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.util.ReferenceCountUtil
import net.minecraft.network.play.server.SPacketCustomPayload

// FIXME: use me
class TransactionNettyHandler : ChannelInboundHandlerAdapter() {
    private var inTransaction = false
    private val queue = mutableListOf<Any>()

    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        if (msg is SPacketCustomPayload) {
            if (msg.channelName == "$MOD_ID|TS") {
                inTransaction = true
                return
            }
            if (msg.channelName == "$MOD_ID|TE") {
                if (inTransaction) {
                    queue.forEach {
                        ReferenceCountUtil.release(it)
                        super.channelRead(ctx, it)
                    }
                }
                inTransaction = false
                return
            }
        }
        if (inTransaction) {
            ReferenceCountUtil.retain(msg)
            queue.add(msg)
        } else {
            super.channelRead(ctx, msg)
        }
    }
}