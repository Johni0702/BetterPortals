package de.johni0702.minecraft.betterportals.impl.net

import de.johni0702.minecraft.betterportals.common.portalManager
import de.johni0702.minecraft.betterportals.impl.common.LOGGER
import de.johni0702.minecraft.betterportals.impl.common.sync
import io.netty.buffer.ByteBuf
import net.minecraft.network.PacketBuffer
import net.minecraft.util.ResourceLocation
import net.minecraftforge.fml.common.network.simpleimpl.IMessage
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext

internal class UsePortal(
        var id: ResourceLocation? = null
) : IMessage {

    override fun fromBytes(buf: ByteBuf) {
        with(PacketBuffer(buf)) {
            id = readResourceLocation()
        }
    }

    override fun toBytes(buf: ByteBuf) {
        with(PacketBuffer(buf)) {
            writeResourceLocation(id!!)
        }
    }

    internal class Handler : IMessageHandler<UsePortal, IMessage> {
        override fun onMessage(message: UsePortal, ctx: MessageContext): IMessage? {
            ctx.sync {
                val player = ctx.serverHandler.player
                if (player.connection.targetPos != null) {
                    LOGGER.warn("Ignoring use portal request from $player because they have an outstanding teleport.")
                    return@sync
                }
                val portalAgent = player.world.portalManager.findById(message.id!!)
                if (portalAgent == null) {
                    LOGGER.warn("Received use portal request from $player for unknown portal agent ${message.id}.")
                    return@sync
                }
                portalAgent.serverPortalUsed(player)
            }
            return null
        }
    }
}