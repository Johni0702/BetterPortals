package de.johni0702.minecraft.betterportals.net

import de.johni0702.minecraft.betterportals.BetterPortalsMod
import de.johni0702.minecraft.betterportals.common.sync
import io.netty.buffer.ByteBuf
import net.minecraftforge.fml.common.network.simpleimpl.IMessage
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext

internal class ChangeServerMainView(
        var viewId: Int = 0
) : IMessage {

    override fun fromBytes(buf: ByteBuf) {
        viewId = buf.readInt()
    }

    override fun toBytes(buf: ByteBuf) {
        buf.writeInt(viewId)
    }

    internal class Handler : IMessageHandler<ChangeServerMainView, IMessage> {
        override fun onMessage(message: ChangeServerMainView, ctx: MessageContext): IMessage? {
            ctx.sync { BetterPortalsMod.viewManagerImpl.makeMainViewAck(message.viewId) }
            return null
        }
    }
}