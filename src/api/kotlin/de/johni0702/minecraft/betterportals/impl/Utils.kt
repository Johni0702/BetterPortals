package de.johni0702.minecraft.betterportals.impl

import net.minecraft.network.Packet

//#if MC>=11400
//$$ import io.netty.buffer.ByteBuf
//$$ import net.minecraft.entity.EntityType
//$$ import net.minecraft.tileentity.TileEntityType
//$$ import net.minecraftforge.fml.network.simple.SimpleChannel
//$$ import net.minecraftforge.fml.network.NetworkEvent
//$$ import net.minecraftforge.registries.IForgeRegistry
//$$ import java.util.function.BiConsumer
//#else
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import de.johni0702.minecraft.betterportals.common.server
import net.minecraft.client.Minecraft
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper
import net.minecraftforge.fml.relauncher.Side
//#endif

//#if MC>=11400
//$$ typealias EntityTypeRegistry = IForgeRegistry<EntityType<*>>
//$$ typealias TileEntityTypeRegistry = IForgeRegistry<TileEntityType<*>>
//#else
object EntityTypeRegistry
object TileEntityTypeRegistry
//#endif

//#if MC>=11400
//$$ enum class NetworkDirection(val forge: net.minecraftforge.fml.network.NetworkDirection) {
//$$     TO_CLIENT(net.minecraftforge.fml.network.NetworkDirection.PLAY_TO_CLIENT),
//$$     TO_SERVER(net.minecraftforge.fml.network.NetworkDirection.PLAY_TO_SERVER),
//$$ }
//$$ interface IMessage {
//$$     val direction: NetworkDirection
//$$     fun fromBytes(buf: ByteBuf)
//$$     fun toBytes(buf: ByteBuf)
//$$ }
//$$
//$$ typealias MessageContext = NetworkEvent.Context
//$$
//$$ interface IMessageHandler<IN> {
//$$     fun handle(message: IN, ctx: MessageContext)
//$$     fun new(): IN
//$$ }
//$$ inline fun <reified T: IMessage> SimpleChannel.register(handler: IMessageHandler<T>, id: Int) {
//$$     messageBuilder(T::class.java, id)
//$$             .encoder { msg, buf -> msg.toBytes(buf) }
//$$             .decoder { buf -> handler.new().also { it.fromBytes(buf) } }
//$$             .consumer(BiConsumer { msg, ctx ->
//$$                 handler.handle(msg, ctx.get())
//$$                 ctx.get().packetHandled = true
//$$             })
//$$             .add()
//$$ }
//$$ fun SimpleChannel.toVanilla(packet: IMessage): IPacket<*> = toVanillaPacket(packet, packet.direction.forge)
//$$ fun MessageContext.sync(task: () -> Unit) = enqueueWork(task)
//$$ val MessageContext.serverPlayer get() = sender!!
//#else
enum class NetworkDirection(val forge: Side) {
    TO_CLIENT(Side.CLIENT),
    TO_SERVER(Side.SERVER),
}
interface IMessage : net.minecraftforge.fml.common.network.simpleimpl.IMessage {
    val direction: NetworkDirection
}
typealias MessageContext = net.minecraftforge.fml.common.network.simpleimpl.MessageContext
interface IMessageHandler<IN: IMessage> : net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler<IN, IN> {
    override fun onMessage(message: IN, ctx: MessageContext): IN? {
        handle(message, ctx)
        return null
    }
    fun handle(message: IN, ctx: MessageContext)
    fun new(): IN
}
inline fun <reified T: IMessage> SimpleNetworkWrapper.register(handler: IMessageHandler<T>, id: Int) {
    registerMessage(handler, T::class.java, id, handler.new().direction.forge)
}
fun SimpleNetworkWrapper.toVanilla(packet: IMessage): Packet<*> = getPacketFrom(packet)
fun MessageContext.sync(task: () -> Unit) = when(side!!) {
    Side.CLIENT -> syncOnClient(task)
    Side.SERVER -> syncOnServer(task)
}
// Note: must be in separate method so we can access client-only methods/classes
private fun syncOnClient(task: () -> Unit) = Minecraft.getMinecraft().addScheduledTask(task).logFailure()
private fun MessageContext.syncOnServer(task: () -> Unit) = serverHandler.player.serverWorld.server.addScheduledTask(task).logFailure()
private fun <L : ListenableFuture<T>, T> L.logFailure(): L {
    Futures.addCallback(this, object : FutureCallback<T> {
        override fun onSuccess(result: T?) = Unit
        override fun onFailure(t: Throwable) {
            t.printStackTrace()
        }
    })
    return this
}
val MessageContext.serverPlayer get() = serverHandler.player
//#endif