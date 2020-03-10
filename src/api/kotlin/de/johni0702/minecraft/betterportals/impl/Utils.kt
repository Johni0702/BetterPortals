package de.johni0702.minecraft.betterportals.impl

import de.johni0702.minecraft.betterportals.common.PortalAccessor
import de.johni0702.minecraft.betterportals.common.PortalConfiguration
import net.minecraft.block.Block
import net.minecraft.client.renderer.entity.Render
import net.minecraft.client.renderer.entity.RenderManager
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer
import net.minecraft.entity.Entity
import net.minecraft.network.Packet
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.ResourceLocation
import net.minecraft.world.World

//#if FABRIC>=1
//$$ import de.johni0702.minecraft.betterportals.common.PortalManager
//$$ import de.johni0702.minecraft.betterportals.common.orNull
//$$ import de.johni0702.minecraft.view.common.register
//$$ import io.netty.buffer.Unpooled
//$$ import net.fabricmc.fabric.api.block.FabricBlockSettings
//$$ import net.fabricmc.fabric.api.client.render.BlockEntityRendererRegistry
//$$ import net.fabricmc.fabric.api.client.render.EntityRendererRegistry
//$$ import net.fabricmc.fabric.api.entity.FabricEntityTypeBuilder
//$$ import net.fabricmc.fabric.api.network.ClientSidePacketRegistry
//$$ import net.fabricmc.fabric.api.network.PacketContext
//$$ import net.fabricmc.fabric.api.network.PacketRegistry
//$$ import net.fabricmc.fabric.api.network.ServerSidePacketRegistry
//$$ import net.minecraft.util.PacketByteBuf
//$$ import net.minecraft.util.registry.Registry
//#else
import de.johni0702.minecraft.betterportals.common.portalManager
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.client.registry.ClientRegistry
import net.minecraftforge.fml.client.registry.RenderingRegistry
import net.minecraftforge.registries.IForgeRegistry
//#endif

//#if MC>=11400
//$$ import io.netty.buffer.ByteBuf
//$$ import net.minecraft.block.material.Material
//$$ import net.minecraft.block.material.MaterialColor
//$$ import net.minecraft.entity.EntityClassification
//$$ import net.minecraft.entity.EntityType
//$$ import net.minecraft.tileentity.TileEntityType
//$$ import net.minecraft.entity.player.ServerPlayerEntity
//#if FABRIC<=0
//$$ import net.minecraftforge.fml.ModList
//$$ import net.minecraftforge.fml.network.simple.SimpleChannel
//$$ import net.minecraftforge.fml.network.NetworkEvent
//$$ import net.minecraftforge.fml.network.NetworkRegistry
//$$ import java.util.function.BiConsumer
//#endif
//#else
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import de.johni0702.minecraft.betterportals.common.theServer
import net.minecraft.client.Minecraft
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper
import net.minecraftforge.fml.relauncher.Side
//#endif

//#if FABRIC>=1
//$$ fun makeBlockSettings(material: Material, color: MaterialColor? = null, configure: FabricBlockSettings.() -> Unit): Block.Settings =
//$$         FabricBlockSettings.of(material, color ?: material.color).also(configure).build()
//$$ fun <T: Entity> makeEntityType(@Suppress("UNUSED_PARAMETER") id: Identifier, factory: (EntityType<T>, World) -> T, category: EntityCategory, configure: FabricEntityTypeBuilder<T>.() -> Unit): EntityType<T> =
//$$         FabricEntityTypeBuilder.create<T>(category, factory::invoke).apply(configure).build()
//$$ fun <T: Entity> EntityTypeRegistry.registerEntityType(id: Identifier, factory: (EntityType<T>, World) -> T, category: EntityCategory, configure: FabricEntityTypeBuilder<T>.() -> Unit): EntityType<T> =
//$$         makeEntityType(id, factory, category, configure).also { Registry.register(this, id, it) }
//$$ typealias BlockRegistry = Registry<Block>
//$$ typealias EntityTypeRegistry = Registry<EntityType<*>>
//$$ typealias TileEntityTypeRegistry = Registry<BlockEntityType<*>>
//#else
//#if MC>=11400
//$$ fun makeBlockSettings(material: Material, color: MaterialColor? = null, configure: Block.Properties.() -> Unit): Block.Properties =
//$$         Block.Properties.create(material, color ?: material.color).also(configure)
//$$ fun <T: Entity> makeEntityType(id: ResourceLocation, factory: (EntityType<T>, World) -> T, category: EntityClassification, configure: EntityType.Builder<T>.() -> Unit): EntityType<T> =
//$$         EntityType.Builder.create<T>(factory, category).apply(configure).build(id.toString())
//$$ fun <T: Entity> EntityTypeRegistry.registerEntityType(id: ResourceLocation, factory: (EntityType<T>, World) -> T, category: EntityClassification, configure: EntityType.Builder<T>.() -> Unit): EntityType<T> =
//$$         makeEntityType(id, factory, category, configure).apply { setRegistryName(id) }.also { register(it) }
//$$ typealias BlockRegistry = IForgeRegistry<Block>
//$$ typealias EntityTypeRegistry = IForgeRegistry<EntityType<*>>
//$$ typealias TileEntityTypeRegistry = IForgeRegistry<TileEntityType<*>>
//#else
typealias BlockRegistry = IForgeRegistry<Block>
object EntityTypeRegistry
object TileEntityTypeRegistry
//#endif
//#endif

//#if FABRIC>=1
//$$ inline fun <reified T : Entity> registerEntityRenderer(crossinline renderFactory: (EntityRenderDispatcher) -> EntityRenderer<T>) {
//$$     EntityRendererRegistry.INSTANCE.register(T::class.java) { it, _ -> renderFactory(it) }
//$$ }
//$$ inline fun <reified T : BlockEntity> registerBlockEntityRenderer(renderer: BlockEntityRenderer<in T>) {
//$$     BlockEntityRendererRegistry.INSTANCE.register(T::class.java, renderer)
//$$ }
//$$ fun <T> Registry<T>.registerOrReplace(id: Identifier, value: T) {
//$$     val existing = getOrEmpty(id).orNull
//$$     if (existing != null) {
//$$         Registry.register(this, getRawId(existing), id.toString(), value)
//$$     } else {
//$$         Registry.register(this, id, value)
//$$     }
//$$ }
//$$ fun <T : BlockEntity> TileEntityTypeRegistry.register(id: Identifier, type: BlockEntityType<in T>) =
//$$         registerOrReplace(id, type)
//$$ fun BlockRegistry.register(id: Identifier, block: Block) = registerOrReplace(id, block)
//$$ fun registerPortalAccessor(factory: (World) -> PortalAccessor) {
//$$     PortalManager.REGISTER_ACCESSORS_EVENT.register {
//$$         it.registerPortals(factory(it.world))
//$$     }
//$$ }
//#else
inline fun <reified T : Entity> registerEntityRenderer(crossinline renderFactory: (RenderManager) -> Render<T>) {
    RenderingRegistry.registerEntityRenderingHandler(T::class.java) { renderFactory(it) }
}
inline fun <reified T : TileEntity> registerBlockEntityRenderer(renderer: TileEntitySpecialRenderer<in T>) {
    ClientRegistry.bindTileEntitySpecialRenderer(T::class.java, renderer)
}
//#if MC>=11400
//$$ fun <T : TileEntity> TileEntityTypeRegistry.register(id: ResourceLocation, type: TileEntityType<in T>) {
//$$     type.setRegistryName(id).also { register(it) }
//$$ }
//#endif
fun BlockRegistry.register(id: ResourceLocation, block: Block) {
    check(id == block.registryName)
    register(block)
}
fun registerPortalAccessor(factory: (World) -> PortalAccessor) {
    MinecraftForge.EVENT_BUS.register(object {
        @SubscribeEvent
        fun onWorld(event: WorldEvent.Load) {
            val world = event.world
            //#if MC>=11400
            //$$ if (world !is World) return
            //#endif
            world.portalManager.registerPortals(factory(world))
        }
    })
}
//#endif

fun PortalConfig.toConfiguration() = PortalConfiguration(
        { opacity },
        { renderDistMin },
        { renderDistMax },
        { renderDistSizeMultiplier }
)

//#if MC>=11400
//$$ interface IMessage {
//$$     val direction: NetworkDirection
//$$     fun fromBytes(buf: ByteBuf)
//$$     fun toBytes(buf: ByteBuf)
//$$ }
//$$
//$$ interface IMessageHandler<IN> {
//$$     fun handle(message: IN, ctx: MessageContext)
//$$     fun new(): IN
//$$ }
//$$
//#if FABRIC>=1
//$$ fun createNetworkChannel(id: Identifier): SimpleChannel = SimpleChannel(id)
//$$
//$$ class SimpleChannel(private val baseId: Identifier) {
//$$     private val ids = mutableMapOf<Class<out IMessage>, Identifier>()
//$$     private val encoders = mutableMapOf<Class<out IMessage>, (IMessage) -> Packet<*>>()
//$$
//$$     fun <T: IMessage> registerImpl(handler: IMessageHandler<T>, id: Int, type: Class<out T>) {
//$$         val direction = handler.new().direction
//$$         val fullId = Identifier(baseId.namespace, baseId.path + '/' + id)
//$$         ids[type] = fullId
//$$         encoders[type] = { message ->
//$$             direction.senderRegistry.toPacket(fullId, PacketByteBuf(Unpooled.buffer().also { message.toBytes(it) }))
//$$         }
//$$         direction.receiverRegistry.register(fullId) { context, buf ->
//$$             val message = handler.new()
//$$             message.fromBytes(buf)
//$$             handler.handle(message, context)
//$$         }
//$$     }
//$$
//$$     fun toVanillaImpl(message: IMessage): Packet<*> = encoders[message.javaClass]!!(message)
//$$     fun sendToServer(message: IMessage) = ClientSidePacketRegistry.INSTANCE.sendToServer(toVanillaImpl(message))
//$$ }
//$$ enum class NetworkDirection(val senderRegistry: PacketRegistry, val receiverRegistry: PacketRegistry) {
//$$     TO_CLIENT(ServerSidePacketRegistry.INSTANCE, ClientSidePacketRegistry.INSTANCE),
//$$     TO_SERVER(ClientSidePacketRegistry.INSTANCE, ServerSidePacketRegistry.INSTANCE),
//$$ }
//$$ typealias MessageContext = PacketContext
//$$ inline fun <reified T: IMessage> SimpleChannel.register(handler: IMessageHandler<T>, id: Int) {
//$$     registerImpl(handler, id, T::class.java)
//$$ }
//$$ fun SimpleChannel.toVanilla(packet: IMessage): Packet<*> = toVanillaImpl(packet)
//$$ fun MessageContext.sync(task: () -> Unit) {
//$$     if (!taskQueue.isOnThread) {
//$$         taskQueue.execute(task)
//$$     } else {
//$$         task()
//$$     }
//$$ }
//$$ val MessageContext.serverPlayer get() = player as ServerPlayerEntity
//#else
//$$ fun createNetworkChannel(id: ResourceLocation): SimpleChannel {
//$$     val modVersion = ModList.get().getModContainerById("betterportals").get()!!.modInfo.version.toString()
//$$     return NetworkRegistry.newSimpleChannel(
//$$             id,
//$$             { modVersion },
//$$             { it == modVersion },
//$$             { it == modVersion }
//$$     )
//$$ }
//$$ enum class NetworkDirection(val forge: net.minecraftforge.fml.network.NetworkDirection) {
//$$     TO_CLIENT(net.minecraftforge.fml.network.NetworkDirection.PLAY_TO_CLIENT),
//$$     TO_SERVER(net.minecraftforge.fml.network.NetworkDirection.PLAY_TO_SERVER),
//$$ }
//$$ typealias MessageContext = NetworkEvent.Context
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
//$$ val MessageContext.serverPlayer: ServerPlayerEntity get() = sender!!
//#endif
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
private fun MessageContext.syncOnServer(task: () -> Unit) = serverHandler.player.serverWorld.theServer.addScheduledTask(task).logFailure()
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