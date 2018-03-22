package de.johni0702.minecraft.betterportals.net

import de.johni0702.minecraft.betterportals.BetterPortalsMod
import de.johni0702.minecraft.betterportals.common.sync
import io.netty.buffer.ByteBuf
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.WorldClient
import net.minecraft.network.PacketBuffer
import net.minecraft.world.EnumDifficulty
import net.minecraft.world.GameType
import net.minecraft.world.WorldSettings
import net.minecraft.world.WorldType
import net.minecraftforge.fml.common.network.simpleimpl.IMessage
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext

class CreateView(
        var viewId: Int = 0,
        var dimensionID: Int = 0,
        var difficulty: EnumDifficulty? = null,
        var gameType: GameType? = null,
        var worldType: WorldType? = null
) : IMessage {

    override fun fromBytes(byteBuf: ByteBuf) {
        val buf = PacketBuffer(byteBuf)
        viewId = buf.readInt()
        dimensionID = buf.readInt()
        difficulty = EnumDifficulty.getDifficultyEnum(buf.readUnsignedByte().toInt())
        gameType = GameType.getByID(buf.readUnsignedByte().toInt())
        worldType = WorldType.parseWorldType(buf.readString(16))
        if (worldType == null) {
            worldType = WorldType.DEFAULT
        }
    }

    override fun toBytes(byteBuf: ByteBuf) {
        val buf = PacketBuffer(byteBuf)
        buf.writeInt(viewId)
        buf.writeInt(dimensionID)
        buf.writeByte(difficulty!!.difficultyId)
        buf.writeByte(gameType!!.id)
        buf.writeString(worldType!!.name)
    }

    internal class Handler : IMessageHandler<CreateView, IMessage> {
        override fun onMessage(message: CreateView, ctx: MessageContext): IMessage? {
            ctx.sync {
                val mc = Minecraft.getMinecraft()
                val world = WorldClient(mc.connection!!,
                        WorldSettings(0L,
                                message.gameType!!,
                                false,
                                mc.world.worldInfo.isHardcoreModeEnabled,
                                message.worldType!!),
                        message.dimensionID,
                        message.difficulty!!,
                        mc.mcProfiler)
                BetterPortalsMod.viewManagerImpl.createView(message.viewId, world)
            }
            return null
        }
    }
}
