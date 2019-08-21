package de.johni0702.minecraft.view.impl.net

import de.johni0702.minecraft.view.impl.ClientViewAPIImpl
import de.johni0702.minecraft.view.impl.common.clientSyncIgnoringView
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

internal class CreateWorld(
        var dimensionID: Int = 0,
        var difficulty: EnumDifficulty? = null,
        var gameType: GameType? = null,
        var worldType: WorldType? = null
) : IMessage {

    override fun fromBytes(byteBuf: ByteBuf) {
        val buf = PacketBuffer(byteBuf)
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
        buf.writeInt(dimensionID)
        buf.writeByte(difficulty!!.difficultyId)
        buf.writeByte(gameType!!.id)
        buf.writeString(worldType!!.name)
    }

    internal class Handler : IMessageHandler<CreateWorld, IMessage> {
        override fun onMessage(message: CreateWorld, ctx: MessageContext): IMessage? {
            clientSyncIgnoringView {
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
                ClientViewAPIImpl.viewManagerImpl.createState(world)
            }
            return null
        }
    }
}
