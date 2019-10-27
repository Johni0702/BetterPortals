package de.johni0702.minecraft.betterportals.impl.net

import de.johni0702.minecraft.betterportals.common.portalManager
import de.johni0702.minecraft.betterportals.impl.IMessage
import de.johni0702.minecraft.betterportals.impl.IMessageHandler
import de.johni0702.minecraft.betterportals.impl.MessageContext
import de.johni0702.minecraft.betterportals.impl.NetworkDirection
import de.johni0702.minecraft.betterportals.impl.accessors.AccNetHandlerPlayServer
import de.johni0702.minecraft.betterportals.impl.common.LOGGER
import de.johni0702.minecraft.betterportals.impl.serverPlayer
import de.johni0702.minecraft.betterportals.impl.sync
import io.netty.buffer.ByteBuf
import net.minecraft.network.PacketBuffer
import net.minecraft.util.ResourceLocation

internal class UsePortal(
        var id: ResourceLocation? = null
) : IMessage {
    override val direction = NetworkDirection.TO_SERVER

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

    internal class Handler : IMessageHandler<UsePortal> {
        override fun new(): UsePortal = UsePortal()

        override fun handle(message: UsePortal, ctx: MessageContext) {
            ctx.sync {
                val player = ctx.serverPlayer
                if ((player.connection as AccNetHandlerPlayServer).targetPos != null) {
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
        }
    }
}