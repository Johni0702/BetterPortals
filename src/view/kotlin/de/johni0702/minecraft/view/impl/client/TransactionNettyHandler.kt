package de.johni0702.minecraft.view.impl.client

import de.johni0702.minecraft.view.impl.net.Transaction
import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.util.ReferenceCountUtil
import net.minecraft.network.play.server.SPacketCustomPayload

//#if MC>=11400
//$$ import net.minecraft.util.ResourceLocation
//#endif

internal class TransactionNettyHandler : ChannelInboundHandlerAdapter() {
    private var inTransaction = 0
    private val queue = mutableListOf<Any>()

    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        if (msg is SPacketCustomPayload) {
            if (msg.channelName == CHANNEL_START) {
                inTransaction++
                return
            }
            if (msg.channelName == CHANNEL_END) {
                if (inTransaction <= 1) {
                    Transaction.lock()
                    queue.forEach {
                        ReferenceCountUtil.release(it)
                        super.channelRead(ctx, it)
                    }
                    queue.clear()
                    Transaction.unlock()
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
        //#if MC>=11400
        //$$ val CHANNEL_START = ResourceLocation("betterportals", "transaction_start")
        //$$ val CHANNEL_END = ResourceLocation("betterportals", "transaction_end")
        //#else
        const val CHANNEL_START = "BP|TS"
        const val CHANNEL_END = "BP|TE"
        //#endif

        @JvmStatic
        fun inject(channel: Channel) {
            channel.pipeline().addBefore("packet_handler", "transaction_handler", TransactionNettyHandler())
        }
    }
}