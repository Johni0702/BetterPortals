package de.johni0702.minecraft.betterportals.impl.transition.net

import de.johni0702.minecraft.betterportals.impl.IMessage
import de.johni0702.minecraft.betterportals.impl.IMessageHandler
import de.johni0702.minecraft.betterportals.impl.MessageContext
import de.johni0702.minecraft.betterportals.impl.NetworkDirection
import de.johni0702.minecraft.betterportals.impl.serverPlayer
import de.johni0702.minecraft.betterportals.impl.sync
import de.johni0702.minecraft.betterportals.impl.transition.server.DimensionTransitionHandler
import de.johni0702.minecraft.view.server.worldsManager
import io.netty.buffer.ByteBuf

/**
 * Sent from the client when a dimension change initiated by a [TransferToDimension] message has been completed.
 */
internal class TransferToDimensionDone(var id: Int = 0) : IMessage {
    override val direction = NetworkDirection.TO_SERVER

    override fun fromBytes(buf: ByteBuf) {
        id = buf.readInt()
    }

    override fun toBytes(buf: ByteBuf) {
        buf.writeInt(id)
    }

    internal class Handler : IMessageHandler<TransferToDimensionDone> {
        override fun new(): TransferToDimensionDone = TransferToDimensionDone()

        override fun handle(message: TransferToDimensionDone, ctx: MessageContext) {
            ctx.sync {
                DimensionTransitionHandler.views.computeIfPresent(ctx.serverPlayer.worldsManager) { _, views ->
                    views.remove(message.id)?.dispose()
                    if (views.isEmpty()) null else views
                }
            }
        }
    }
}