package de.johni0702.minecraft.betterportals.impl.net

import de.johni0702.minecraft.betterportals.common.portalManager
import de.johni0702.minecraft.betterportals.common.readEnum
import de.johni0702.minecraft.betterportals.common.writeEnum
import de.johni0702.minecraft.betterportals.impl.IMessage
import de.johni0702.minecraft.betterportals.impl.IMessageHandler
import de.johni0702.minecraft.betterportals.impl.MessageContext
import de.johni0702.minecraft.betterportals.impl.NetworkDirection
import de.johni0702.minecraft.betterportals.impl.common.LOGGER
import de.johni0702.minecraft.betterportals.impl.sync
import io.netty.buffer.ByteBuf
import net.minecraft.client.Minecraft
import net.minecraft.network.PacketBuffer
import net.minecraft.util.ResourceLocation

internal class EntityUsePortal(
        var phase: Phase = Phase.BEFORE,
        var entityId: Int = 0,
        var portalId: ResourceLocation? = null
) : IMessage {
    override val direction = NetworkDirection.TO_CLIENT

    override fun fromBytes(buf: ByteBuf) {
        with(PacketBuffer(buf)) {
            phase = readEnum()
            entityId = readVarInt()
            portalId = readResourceLocation()
        }
    }

    override fun toBytes(buf: ByteBuf) {
        with(PacketBuffer(buf)) {
            writeEnum(phase)
            writeVarInt(entityId)
            writeResourceLocation(portalId!!)
        }
    }

    internal class Handler : IMessageHandler<EntityUsePortal> {
        override fun new(): EntityUsePortal = EntityUsePortal()

        override fun handle(message: EntityUsePortal, ctx: MessageContext) {
            ctx.sync {
                val world = Minecraft.getMinecraft().world
                val portal = world.portalManager.findById(message.portalId!!)
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
        }
    }

    enum class Phase {
        BEFORE, AFTER
    }
}