package de.johni0702.minecraft.betterportals.impl.net

import de.johni0702.minecraft.betterportals.common.portalManager
import de.johni0702.minecraft.betterportals.impl.common.LOGGER
import de.johni0702.minecraft.betterportals.impl.common.sync
import de.johni0702.minecraft.view.client.viewManager
import io.netty.buffer.ByteBuf
import net.minecraft.client.Minecraft
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.network.PacketBuffer
import net.minecraft.util.ResourceLocation
import net.minecraftforge.fml.common.network.simpleimpl.IMessage
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext

internal class LinkPortal(
        var portalId: ResourceLocation? = null,
        var nbt: NBTTagCompound? = null,
        var viewId: Int? = 0
) : IMessage {

    override fun fromBytes(buf: ByteBuf) {
        with(PacketBuffer(buf)) {
            portalId = readResourceLocation()
            nbt = readCompoundTag()
            viewId = if (readBoolean()) {
                readVarInt()
            } else {
                null
            }
        }
    }

    override fun toBytes(buf: ByteBuf) {
        with(PacketBuffer(buf)) {
            writeResourceLocation(portalId!!)
            writeCompoundTag(nbt)
            val viewId = viewId
            if (viewId != null) {
                writeBoolean(true)
                writeVarInt(viewId)
            } else {
                writeBoolean(false)
            }
            return
        }
    }

    internal class Handler : IMessageHandler<LinkPortal, IMessage> {
        override fun onMessage(message: LinkPortal, ctx: MessageContext): IMessage? {
            ctx.sync {
                val viewManager = Minecraft.getMinecraft().viewManager ?: return@sync
                val world = ctx.clientHandler.clientWorldController ?: return@sync
                val agent = world.portalManager.findById(message.portalId!!)
                if (agent == null) {
                    LOGGER.warn("Received sync message for unknown portal agent ${message.portalId}")
                    return@sync
                }
                message.nbt?.let { agent.portal.readPortalFromNBT(it) }

                val viewId = message.viewId
                if (viewId != null) {
                    val view = viewManager.views.find { it.id == message.viewId }
                    if (view == null) {
                        LOGGER.warn("Received sync message with unknown view id ${message.viewId} for portal $agent")
                        return@sync
                    }
                    agent.view = view
                } else {
                    agent.view = null
                }
            }
            return null
        }
    }
}