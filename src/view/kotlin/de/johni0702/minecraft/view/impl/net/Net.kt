package de.johni0702.minecraft.view.impl.net

import de.johni0702.minecraft.view.impl.MOD_ID
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
            registerMessage(CreateWorld.Handler(), CreateWorld::class.java, ++nextId, Side.CLIENT)
            registerMessage(WorldData.Handler(), WorldData::class.java, ++nextId, Side.CLIENT)
            registerMessage(DestroyWorld.Handler(), DestroyWorld::class.java, ++nextId, Side.CLIENT)
            registerMessage(ChangeServerMainWorld.Handler(), ChangeServerMainWorld::class.java, ++nextId, Side.CLIENT)
            registerMessage(Transaction.Handler(), Transaction::class.java, ++nextId, Side.CLIENT)
        }
    }

}

internal fun IMessage.toPacket(): Packet<*> = Net.INSTANCE.getPacketFrom(this)
internal fun IMessage.sendTo(players: Iterable<EntityPlayerMP>)
        = toPacket().let { packet -> players.forEach { it.connection.sendPacket(packet) } }
internal fun IMessage.sendTo(vararg players: EntityPlayerMP)
        = toPacket().let { packet -> players.forEach { it.connection.sendPacket(packet) } }
