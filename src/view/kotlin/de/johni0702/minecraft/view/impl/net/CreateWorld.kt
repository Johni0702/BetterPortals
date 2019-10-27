package de.johni0702.minecraft.view.impl.net

import de.johni0702.minecraft.betterportals.common.DimensionId
import de.johni0702.minecraft.betterportals.common.toDimensionId
import de.johni0702.minecraft.betterportals.common.toIntId
import de.johni0702.minecraft.betterportals.impl.IMessage
import de.johni0702.minecraft.betterportals.impl.IMessageHandler
import de.johni0702.minecraft.betterportals.impl.MessageContext
import de.johni0702.minecraft.betterportals.impl.NetworkDirection
import de.johni0702.minecraft.view.impl.ClientViewAPIImpl
import de.johni0702.minecraft.view.impl.common.clientSyncIgnoringView
import io.netty.buffer.ByteBuf
import net.minecraft.network.PacketBuffer
import net.minecraft.world.EnumDifficulty
import net.minecraft.world.GameType
import net.minecraft.world.WorldType

internal class CreateWorld(
        var dimensionID: DimensionId = 0.toDimensionId()!!,
        var providerID: String? = null,
        var difficulty: EnumDifficulty? = null,
        var gameType: GameType? = null,
        var worldType: WorldType? = null
) : IMessage {
    override val direction = NetworkDirection.TO_CLIENT

    override fun fromBytes(byteBuf: ByteBuf) {
        val buf = PacketBuffer(byteBuf)
        dimensionID = buf.readInt().toDimensionId()!!
        providerID = buf.readString(Short.MAX_VALUE.toInt())
        difficulty = EnumDifficulty.getDifficultyEnum(buf.readUnsignedByte().toInt())
        gameType = GameType.getByID(buf.readUnsignedByte().toInt())
        worldType = WorldType.parseWorldType(buf.readString(16))
        if (worldType == null) {
            worldType = WorldType.DEFAULT
        }
    }

    override fun toBytes(byteBuf: ByteBuf) {
        val buf = PacketBuffer(byteBuf)
        buf.writeInt(dimensionID.toIntId())
        buf.writeString(providerID!!)
        buf.writeByte(difficulty!!.difficultyId)
        buf.writeByte(gameType!!.id)
        buf.writeString(worldType!!.name)
    }

    internal class Handler : IMessageHandler<CreateWorld> {
        override fun new(): CreateWorld = CreateWorld()

        override fun handle(message: CreateWorld, ctx: MessageContext) {
            clientSyncIgnoringView {
                ClientViewAPIImpl.viewManagerImpl.createState(message)
            }
        }
    }
}
