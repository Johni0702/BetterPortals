package de.johni0702.minecraft.betterportals.net

import de.johni0702.minecraft.betterportals.LOGGER
import de.johni0702.minecraft.betterportals.common.entity.AbstractPortalEntity
import de.johni0702.minecraft.betterportals.common.sync
import io.netty.buffer.ByteBuf
import net.minecraftforge.fml.common.network.simpleimpl.IMessage
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext

class UsePortal(
        var entityId: Int = 0
) : IMessage {

    override fun fromBytes(buf: ByteBuf) {
        entityId = buf.readInt()
    }

    override fun toBytes(buf: ByteBuf) {
        buf.writeInt(entityId)
    }

    internal class Handler : IMessageHandler<UsePortal, IMessage> {
        override fun onMessage(message: UsePortal, ctx: MessageContext): IMessage? {
            ctx.sync {
                val player = ctx.serverHandler.player
                if (player.connection.targetPos != null) {
                    LOGGER.warn("Ignoring use portal request from $player because they have an outstanding teleport.")
                    return@sync
                }
                val portalEntity = player.world.getEntityByID(message.entityId) as? AbstractPortalEntity
                if (portalEntity == null) {
                    LOGGER.warn("Received use portal request from $player for unknown entity ${message.entityId}.")
                    return@sync
                }
                portalEntity.usePortal(player)
            }
            return null
        }
    }
}