package de.johni0702.minecraft.view.impl.net

import de.johni0702.minecraft.view.impl.ClientViewAPIImpl
import de.johni0702.minecraft.view.impl.LOGGER
import de.johni0702.minecraft.view.impl.common.sync
import io.netty.buffer.ByteBuf
import net.minecraftforge.fml.common.network.simpleimpl.IMessage
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext

internal class DestroyView(
        var viewId: Int = 0
) : IMessage {

    override fun fromBytes(buf: ByteBuf) {
        viewId = buf.readInt()
    }

    override fun toBytes(buf: ByteBuf) {
        buf.writeInt(viewId)
    }

    internal class Handler : IMessageHandler<DestroyView, IMessage> {
        override fun onMessage(message: DestroyView, ctx: MessageContext): IMessage? {
            ctx.sync {
                val manager = ClientViewAPIImpl.viewManagerImpl
                val view = manager.views.find { it.id == message.viewId }
                if (view == null) {
                    LOGGER.warn("Received destroy view message for unknown view with id ${message.viewId}")
                    return@sync
                }
                manager.destroyView(view)
            }
            return null
        }
    }
}
