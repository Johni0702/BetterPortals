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
internal class TransferToDimensionDone : IMessage {

    override fun fromBytes(buf: ByteBuf) {
    }

    override fun toBytes(buf: ByteBuf) {
    }

    internal class Handler : IMessageHandler<TransferToDimensionDone, IMessage> {
        override fun onMessage(message: TransferToDimensionDone, ctx: MessageContext): IMessage? {
            ctx.sync {
                DimensionTransitionHandler.views.remove(ctx.serverHandler.player.worldsManager)?.dispose()
            }
            return null
        }
    }
}