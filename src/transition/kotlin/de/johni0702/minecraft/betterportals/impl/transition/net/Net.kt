package de.johni0702.minecraft.betterportals.impl.transition.net

import de.johni0702.minecraft.betterportals.impl.IMessage
import de.johni0702.minecraft.betterportals.impl.register
import de.johni0702.minecraft.betterportals.impl.toVanilla
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.network.Packet
import net.minecraftforge.fml.common.network.NetworkRegistry

//#if MC>=11400
//$$ import net.minecraft.util.ResourceLocation
//$$ import net.minecraftforge.api.distmarker.Dist
//$$ import net.minecraftforge.fml.ModList
//$$ import net.minecraftforge.fml.network.NetworkDirection
//#else
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper
//#endif

internal object Net {
    //#if MC>=11400
    //$$ val MOD_VERSION = ModList.get().getModContainerById("betterportals").get()!!.modInfo.version.toString()
    //$$ val INSTANCE = NetworkRegistry.newSimpleChannel(
    //$$         ResourceLocation("betterportals", "transition"),
    //$$         { MOD_VERSION },
    //$$         { it == MOD_VERSION },
    //$$         { it == MOD_VERSION }
    //$$ )
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
