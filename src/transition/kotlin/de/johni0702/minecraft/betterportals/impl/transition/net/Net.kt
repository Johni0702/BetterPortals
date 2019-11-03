package de.johni0702.minecraft.betterportals.impl.transition.net

import de.johni0702.minecraft.betterportals.impl.IMessage
import de.johni0702.minecraft.betterportals.impl.register
import de.johni0702.minecraft.betterportals.impl.toVanilla
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.network.Packet

//#if MC>=11400
//$$ import de.johni0702.minecraft.betterportals.impl.createNetworkChannel
//$$ import net.minecraft.util.ResourceLocation
//#else
import net.minecraftforge.fml.common.network.NetworkRegistry
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper
//#endif

internal object Net {
    //#if MC>=11400
    //$$ val INSTANCE = createNetworkChannel(ResourceLocation("betterportals", "transition"))
    //#else
    val INSTANCE: SimpleNetworkWrapper = NetworkRegistry.INSTANCE.newSimpleChannel("BP/transition")
    //#endif

    init {
        var nextId = 0
        with(INSTANCE) {
            register(TransferToDimension.Handler(), ++nextId)
            register(TransferToDimensionDone.Handler(), ++nextId)
        }
    }

}

fun IMessage.toPacket(): Packet<*> = Net.INSTANCE.toVanilla(this)
fun IMessage.sendTo(players: Iterable<EntityPlayerMP>)
        = toPacket().let { packet -> players.forEach { it.connection.sendPacket(packet) } }
fun IMessage.sendTo(vararg players: EntityPlayerMP)
        = toPacket().let { packet -> players.forEach { it.connection.sendPacket(packet) } }
