package de.johni0702.minecraft.view.impl.net

import de.johni0702.minecraft.betterportals.common.DimensionId
import de.johni0702.minecraft.betterportals.common.toDimensionId
import de.johni0702.minecraft.betterportals.common.toIntId
import de.johni0702.minecraft.betterportals.impl.IMessage
import de.johni0702.minecraft.betterportals.impl.IMessageHandler
import de.johni0702.minecraft.betterportals.impl.MessageContext
import de.johni0702.minecraft.betterportals.impl.NetworkDirection
import de.johni0702.minecraft.view.impl.ClientViewAPIImpl
import de.johni0702.minecraft.view.impl.common.clientSyncIgnoringView
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.util.AbstractReferenceCounted
import io.netty.util.ReferenceCounted
import net.minecraft.network.PacketBuffer

internal class WorldData(
        var dimensionId: DimensionId = 0.toDimensionId()!!,
        var data: ByteBuf = Unpooled.EMPTY_BUFFER
) : IMessage, AbstractReferenceCounted() {
    override val direction = NetworkDirection.TO_CLIENT

    override fun fromBytes(buf: ByteBuf) {
        with(PacketBuffer(buf)) {
            dimensionId = readVarInt().toDimensionId()!!
            data = readBytes(buf.readableBytes())
        }
    }

    override fun toBytes(buf: ByteBuf) {
        with(PacketBuffer(buf)) {
            writeVarInt(dimensionId.toIntId())
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

    internal class Handler : IMessageHandler<WorldData> {
        override fun new(): WorldData = WorldData()
        override fun handle(message: WorldData, ctx: MessageContext) {
            message.retain()
            clientSyncIgnoringView {
                try {
                    ClientViewAPIImpl.viewManagerImpl.handleWorldData(message.dimensionId, message.data)
                } finally {
                    message.release()
                }
            }
        }
    }
}
