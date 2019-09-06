package de.johni0702.minecraft.view.impl.net

import de.johni0702.minecraft.betterportals.common.DimensionId
import de.johni0702.minecraft.betterportals.common.toDimensionId
import de.johni0702.minecraft.betterportals.common.toIntId
import de.johni0702.minecraft.betterportals.impl.IMessage
import de.johni0702.minecraft.betterportals.impl.IMessageHandler
import de.johni0702.minecraft.betterportals.impl.MessageContext
import de.johni0702.minecraft.betterportals.impl.NetworkDirection
import de.johni0702.minecraft.view.impl.ClientViewAPIImpl
import de.johni0702.minecraft.view.impl.LOGGER
import de.johni0702.minecraft.view.impl.common.clientSyncIgnoringView
import io.netty.buffer.ByteBuf

internal class DestroyWorld(
        var dimensionID: DimensionId = 0.toDimensionId()!!
) : IMessage {
    override val direction = NetworkDirection.TO_CLIENT

    override fun fromBytes(buf: ByteBuf) {
        dimensionID = buf.readInt().toDimensionId()!!
    }

    override fun toBytes(buf: ByteBuf) {
        buf.writeInt(dimensionID.toIntId())
    }

    internal class Handler : IMessageHandler<DestroyWorld> {
        override fun new(): DestroyWorld = DestroyWorld()

        override fun handle(message: DestroyWorld, ctx: MessageContext) {
            clientSyncIgnoringView {
                val manager = ClientViewAPIImpl.viewManagerImpl
                val view = manager.views.find { it.dimension == message.dimensionID }
                if (view == null) {
                    LOGGER.warn("Received destroy world message for unknown dimension with id ${message.dimensionID}")
                    return@clientSyncIgnoringView
                }
                manager.destroyState(view)
            }
        }
    }
}
