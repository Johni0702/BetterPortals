package de.johni0702.minecraft.betterportals.impl.net

import de.johni0702.minecraft.betterportals.impl.common.MOD_ID
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.network.Packet
import net.minecraftforge.fml.common.network.NetworkRegistry
import net.minecraftforge.fml.common.network.simpleimpl.IMessage
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper
import net.minecraftforge.fml.relauncher.Side

internal object Net {
    val INSTANCE: SimpleNetworkWrapper = NetworkRegistry.INSTANCE.newSimpleChannel(MOD_ID)

    init {
        var nextId = 0
        with(INSTANCE) {
            registerMessage(UsePortal.Handler(), UsePortal::class.java, ++nextId, Side.SERVER)
            registerMessage(LinkPortal.Handler(), LinkPortal::class.java, ++nextId, Side.CLIENT)
            registerMessage(EntityUsePortal.Handler(), EntityUsePortal::class.java, ++nextId, Side.CLIENT)
            registerMessage(TransferToDimension.Handler(), TransferToDimension::class.java, ++nextId, Side.CLIENT)
            registerMessage(TransferToDimensionDone.Handler(), TransferToDimensionDone::class.java, ++nextId, Side.SERVER)
        }
    }

}

fun IMessage.toPacket(): Packet<*> = Net.INSTANCE.getPacketFrom(this)
fun IMessage.sendTo(players: Iterable<EntityPlayerMP>)
        = toPacket().let { packet -> players.forEach { it.connection.sendPacket(packet) } }
fun IMessage.sendTo(vararg players: EntityPlayerMP)
        = toPacket().let { packet -> players.forEach { it.connection.sendPacket(packet) } }
