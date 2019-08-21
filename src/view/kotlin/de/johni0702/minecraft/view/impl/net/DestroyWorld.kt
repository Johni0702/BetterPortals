package de.johni0702.minecraft.view.impl.net

import de.johni0702.minecraft.view.impl.ClientViewAPIImpl
import de.johni0702.minecraft.view.impl.LOGGER
import de.johni0702.minecraft.view.impl.common.clientSyncIgnoringView
import io.netty.buffer.ByteBuf
import net.minecraftforge.fml.common.network.simpleimpl.IMessage
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext

internal class DestroyWorld(
        var dimensionID: Int = 0
) : IMessage {

    override fun fromBytes(buf: ByteBuf) {
        dimensionID = buf.readInt()
    }

    override fun toBytes(buf: ByteBuf) {
        buf.writeInt(dimensionID)
    }

    internal class Handler : IMessageHandler<DestroyWorld, IMessage> {
        override fun onMessage(message: DestroyWorld, ctx: MessageContext): IMessage? {
            clientSyncIgnoringView {
                val manager = ClientViewAPIImpl.viewManagerImpl
                val view = manager.views.find { it.dimension == message.dimensionID }
                if (view == null) {
                    LOGGER.warn("Received destroy world message for unknown dimension with id ${message.dimensionID}")
                    return@clientSyncIgnoringView
                }
                manager.destroyState(view)
            }
            return null
        }
    }
}
