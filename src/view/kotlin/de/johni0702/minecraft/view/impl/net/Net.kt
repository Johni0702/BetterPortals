package de.johni0702.minecraft.view.impl.net

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

object Net {
    //#if MC>=11400
    //$$ @JvmField
    //$$ val CHANNEL = ResourceLocation("betterportals", "view")
    //$$ val INSTANCE = createNetworkChannel(CHANNEL)
    //#else
    val INSTANCE: SimpleNetworkWrapper = NetworkRegistry.INSTANCE.newSimpleChannel("BP/view")
    //#endif

    init {
        var nextId = 0
        with(INSTANCE) {
            register(CreateWorld.Handler(), ++nextId)
            register(WorldData.Handler(), ++nextId)
            register(DestroyWorld.Handler(), ++nextId)
            register(ChangeServerMainWorld.Handler(), ++nextId)
        }
    }

}

internal fun IMessage.toPacket(): Packet<*> = Net.INSTANCE.toVanilla(this)
internal fun IMessage.sendTo(players: Iterable<EntityPlayerMP>)
        = toPacket().let { packet -> players.forEach { it.connection.sendPacket(packet) } }
internal fun IMessage.sendTo(vararg players: EntityPlayerMP)
        = toPacket().let { packet -> players.forEach { it.connection.sendPacket(packet) } }
