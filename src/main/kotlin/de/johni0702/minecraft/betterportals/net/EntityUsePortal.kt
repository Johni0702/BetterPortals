package de.johni0702.minecraft.betterportals.net

import de.johni0702.minecraft.betterportals.LOGGER
import de.johni0702.minecraft.betterportals.common.entity.AbstractPortalEntity
import de.johni0702.minecraft.betterportals.common.readEnum
import de.johni0702.minecraft.betterportals.common.sync
import de.johni0702.minecraft.betterportals.common.writeEnum
import io.netty.buffer.ByteBuf
import net.minecraft.network.PacketBuffer
import net.minecraftforge.fml.common.network.simpleimpl.IMessage
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext

class EntityUsePortal(
        var phase: Phase = Phase.BEFORE,
        var entityId: Int = 0,
        var portalId: Int = 0
) : IMessage {
    override fun fromBytes(buf: ByteBuf) {
        with(PacketBuffer(buf)) {
            phase = readEnum()
            entityId = readVarInt()
            portalId = readVarInt()
        }
    }

    override fun toBytes(buf: ByteBuf) {
        with(PacketBuffer(buf)) {
            writeEnum(phase)
            writeVarInt(entityId)
            writeVarInt(portalId)
        }
    }

    internal class Handler : IMessageHandler<EntityUsePortal, IMessage> {
        override fun onMessage(message: EntityUsePortal, ctx: MessageContext): IMessage? {
            ctx.sync {
                val world = ctx.clientHandler.clientWorldController
                val portal = world.getEntityByID(message.portalId) as? AbstractPortalEntity
                if (portal == null) {
                    LOGGER.warn("Received EntityUsePortal for unknown portal with id ${message.portalId}")
                    return@sync
                }

                when(message.phase) {
                    Phase.BEFORE -> {
                        val entity = world.getEntityByID(message.entityId)
                        if (entity == null) {
                            LOGGER.warn("Received EntityUsePortal for unknown entity with id ${message.entityId}")
                            return@sync
                        }
                        portal.beforeUsePortal(entity)
                    }
                    Phase.AFTER -> portal.afterUsePortal(message.entityId)
                }
            }
            return null
        }
    }

    enum class Phase {
        BEFORE, AFTER
    }
}