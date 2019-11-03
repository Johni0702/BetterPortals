package de.johni0702.minecraft.view.common

//#if FABRIC>=1
//$$ import net.fabricmc.fabric.api.event.EventFactory
//$$ import net.fabricmc.fabric.api.event.Event as FabricEvent
//$$
//$$ interface BPCallback<T> {
//$$     fun handle(event: T)
//$$ }
//$$ @Suppress("FunctionName") // https://stackoverflow.com/a/33610615
//$$ fun <T> BPCallback(handler: (T) -> Unit) = object : BPCallback<T> {
//$$     override fun handle(event: T) {
//$$         handler(event)
//$$     }
//$$ }
//$$ fun <T> FabricEvent<BPCallback<T>>.register(handler: (T) -> Unit) = register(BPCallback(handler))
//$$
//$$ inline fun <reified T> fabricEvent(): FabricEvent<BPCallback<T>> = EventFactory.createArrayBacked<BPCallback<T>>(BPCallback::class.java) { listeners ->
//$$     BPCallback { event ->
//$$         listeners.forEach { it.handle(event) }
//$$     }
//$$ }
//$$
//$$ fun <T> T.post(@Suppress("UNUSED_PARAMETER") fabricEvent: FabricEvent<BPCallback<T>>): T =
//$$         this.also { fabricEvent.invoker().handle(this) }
//$$
//$$ open class Event {
//$$     var isCanceled = false
//$$ }
//$$
//$$ // Dummy annotation so I don't have to add pre-processor statements everywhere (also for documentation)
//$$ annotation class Cancelable
//#else
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.common.eventhandler.Event

@Deprecated("Only functional in Fabric build.")
@Suppress("unused") // type parameter exists for (non-fabric) compile-time validation with post()-calls and fabric-parity
class FabricEvent<T>
@Suppress("DEPRECATION")
fun <T> fabricEvent(): FabricEvent<T> = FabricEvent()

fun <T: Event> T.post(@Suppress("UNUSED_PARAMETER", "DEPRECATION") fabricEvent: FabricEvent<T>) = apply { MinecraftForge.EVENT_BUS.post(this) }
//#endif
