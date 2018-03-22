package de.johni0702.minecraft.betterportals.net

import de.johni0702.minecraft.betterportals.BetterPortalsMod
import de.johni0702.minecraft.betterportals.LOGGER
import de.johni0702.minecraft.betterportals.common.entity.AbstractPortalEntity
import de.johni0702.minecraft.betterportals.common.sync
import io.netty.buffer.ByteBuf
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.network.PacketBuffer
import net.minecraftforge.fml.common.network.simpleimpl.IMessage
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext

class LinkPortal(
        var entityId: Int = 0,
        var nbt: NBTTagCompound? = null,
        var viewId: Int = 0
) : IMessage {

    override fun fromBytes(buf: ByteBuf) {
        with(PacketBuffer(buf)) {
            entityId = readVarInt()
            nbt = readCompoundTag()
            viewId = readVarInt()
        }
    }

    override fun toBytes(buf: ByteBuf) {
        with(PacketBuffer(buf)) {
            writeVarInt(entityId)
            writeCompoundTag(nbt)
            writeVarInt(viewId)
            return
        }
    }

    internal class Handler : IMessageHandler<LinkPortal, IMessage> {
        override fun onMessage(message: LinkPortal, ctx: MessageContext): IMessage? {
            ctx.sync {
                val world = ctx.clientHandler.clientWorldController
                val entity = world.getEntityByID(message.entityId) as? AbstractPortalEntity
                if (entity == null) {
                    LOGGER.warn("Received sync message for unknown portal entity ${message.entityId}")
                    return@sync
                }
                message.nbt?.let { entity.readPortalFromNBT(it) }

                val view = BetterPortalsMod.viewManager.views.find { it.id == message.viewId }
                if (view == null) {
                    LOGGER.warn("Received sync message with unknown view id ${message.viewId} for portal $entity")
                    return@sync
                }
                entity.view = view
            }
            return null
        }
    }
}