package de.johni0702.minecraft.view.impl.net

import de.johni0702.minecraft.view.impl.ClientViewAPIImpl
import de.johni0702.minecraft.view.impl.common.clientSyncIgnoringView
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.util.AbstractReferenceCounted
import io.netty.util.ReferenceCounted
import net.minecraft.network.PacketBuffer
import net.minecraftforge.fml.common.network.simpleimpl.IMessage
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext

internal class WorldData(
        var dimensionId: Int = 0,
        var data: ByteBuf = Unpooled.EMPTY_BUFFER
) : IMessage, AbstractReferenceCounted() {

    override fun fromBytes(buf: ByteBuf) {
        with(PacketBuffer(buf)) {
            dimensionId = readVarInt()
            data = readBytes(buf.readableBytes())
        }
    }

    override fun toBytes(buf: ByteBuf) {
        with(PacketBuffer(buf)) {
            writeVarInt(dimensionId)
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

    internal class Handler : IMessageHandler<WorldData, IMessage> {
        override fun onMessage(message: WorldData, ctx: MessageContext): IMessage? {
            message.retain()
            clientSyncIgnoringView {
                try {
                    ClientViewAPIImpl.viewManagerImpl.handleWorldData(message.dimensionId, message.data)
                } finally {
                    message.release()
                }
            }
            return null
        }
    }
}
