package de.johni0702.minecraft.betterportals.impl.transition.net

import de.johni0702.minecraft.betterportals.impl.transition.common.sync
import de.johni0702.minecraft.betterportals.impl.transition.server.DimensionTransitionHandler
import de.johni0702.minecraft.view.server.worldsManager
import io.netty.buffer.ByteBuf
import net.minecraftforge.fml.common.network.simpleimpl.IMessage
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext

/**
 * Sent from the client when a dimension change initiated by a [TransferToDimension] message has been completed.
 */
internal class TransferToDimensionDone(var id: Int = 0) : IMessage {

    override fun fromBytes(buf: ByteBuf) {
        id = buf.readInt()
    }

    override fun toBytes(buf: ByteBuf) {
        buf.writeInt(id)
    }

    internal class Handler : IMessageHandler<TransferToDimensionDone, IMessage> {
        override fun onMessage(message: TransferToDimensionDone, ctx: MessageContext): IMessage? {
            ctx.sync {
                DimensionTransitionHandler.views.computeIfPresent(ctx.serverHandler.player.worldsManager) { _, views ->
                    views.remove(message.id)?.dispose()
                    if (views.isEmpty()) null else views
                }
            }
            return null
        }
    }
}