package de.johni0702.minecraft.betterportals.net

import de.johni0702.minecraft.betterportals.BetterPortalsMod
import de.johni0702.minecraft.betterportals.common.sync
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.util.AbstractReferenceCounted
import io.netty.util.ReferenceCounted
import net.minecraft.network.PacketBuffer
import net.minecraftforge.fml.common.network.simpleimpl.IMessage
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext

class ViewData(
        var viewId: Int = 0,
        var data: ByteBuf = Unpooled.EMPTY_BUFFER
) : IMessage, AbstractReferenceCounted() {

    override fun fromBytes(buf: ByteBuf) {
        with(PacketBuffer(buf)) {
            viewId = readVarInt()
            data = readBytes(buf.readableBytes())
        }
    }

    override fun toBytes(buf: ByteBuf) {
        with(PacketBuffer(buf)) {
            writeVarInt(viewId)
            writeBytes(data)
        }
    }

    override fun touch(hint: Any?): ReferenceCounted {
        data.touch(hint)
        return this
    }

    override fun deallocate() {
        data.release()
    }

    internal class Handler : IMessageHandler<ViewData, IMessage> {
        override fun onMessage(message: ViewData, ctx: MessageContext): IMessage? {
            message.retain()
            ctx.sync {
                try {
                    BetterPortalsMod.viewManagerImpl.handleViewData(message.viewId, message.data)
                } finally {
                    message.release()
                }
            }
            return null
        }
    }
}
