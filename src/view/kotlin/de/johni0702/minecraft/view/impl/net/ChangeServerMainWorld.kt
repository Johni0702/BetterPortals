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

internal class ChangeServerMainWorld(
        var dimensionId: DimensionId = 0.toDimensionId()!!
) : IMessage {
    override val direction = NetworkDirection.TO_CLIENT

    override fun fromBytes(buf: ByteBuf) {
        dimensionId = buf.readInt().toDimensionId()!!
    }

    override fun toBytes(buf: ByteBuf) {
        buf.writeInt(dimensionId.toIntId())
    }

    internal class Handler : IMessageHandler<ChangeServerMainWorld> {
        override fun new(): ChangeServerMainWorld = ChangeServerMainWorld()

        override fun handle(message: ChangeServerMainWorld, ctx: MessageContext) {
            clientSyncIgnoringView {
                ClientViewAPIImpl.viewManagerImpl.makeMainViewAck(message.dimensionId)
            }
        }
    }
}