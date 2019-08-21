package de.johni0702.minecraft.view.impl.net

import de.johni0702.minecraft.view.impl.ClientViewAPIImpl
import de.johni0702.minecraft.view.impl.common.clientSyncIgnoringView
import io.netty.buffer.ByteBuf
import net.minecraftforge.fml.common.network.simpleimpl.IMessage
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext

internal class ChangeServerMainWorld(
        var dimensionId: Int = 0
) : IMessage {

    override fun fromBytes(buf: ByteBuf) {
        dimensionId = buf.readInt()
    }

    override fun toBytes(buf: ByteBuf) {
        buf.writeInt(dimensionId)
    }

    internal class Handler : IMessageHandler<ChangeServerMainWorld, IMessage> {
        override fun onMessage(message: ChangeServerMainWorld, ctx: MessageContext): IMessage? {
            clientSyncIgnoringView {
                ClientViewAPIImpl.viewManagerImpl.makeMainViewAck(message.dimensionId)
            }
            return null
        }
    }
}