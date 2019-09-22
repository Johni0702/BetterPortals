package de.johni0702.minecraft.view.impl.client

import de.johni0702.minecraft.betterportals.common.currentlyOnMainThread
import de.johni0702.minecraft.view.impl.ClientViewAPIImpl
import de.johni0702.minecraft.view.impl.LOGGER
import de.johni0702.minecraft.view.impl.common.maybeValue
import net.minecraft.client.Minecraft
import net.minecraft.network.NetworkManager
import java.util.concurrent.ConcurrentHashMap

//#if MC>=11400
//$$ import net.minecraft.util.concurrent.ThreadTaskExecutor
//#else
import java.util.*
import java.util.concurrent.FutureTask
//#endif

/**
 * Replacement for [Minecraft.scheduledTasks] which tries its best to call the tasks with the correct view where
 * possible.
 */
//#if MC>=11400
//$$ object ViewDemuxingTaskQueue {
//$$     private val mc = Minecraft.getInstance()
//#else
internal class ViewDemuxingTaskQueue(
        private val mc: Minecraft,
        private val backingQueue: Queue<FutureTask<*>> = ArrayDeque()
) : AbstractQueue<FutureTask<*>>() {
//#endif

    private val knownBadCallers = ConcurrentHashMap.newKeySet<StackTraceElement>()

    //#if MC>=11400
    //$$ @JvmStatic
    //$$ fun wrapTask(inner: Runnable): Runnable {
    //#else
    private fun <T> wrapTask(inner: FutureTask<T>): FutureTask<T> {
    //#endif
        val viewManager = ClientViewAPIImpl.viewManagerImpl

        // Determine the view which this task most likely belongs to
        val view: () -> ClientState = when {
            // We already know this one
            inner is ViewWrappedFutureTask ->
                return inner

            // Calling from main thread? must the the currently active view
            mc.currentlyOnMainThread ->
                viewManager.activeView.let { { it } }

            // Calling from any netty thread? must the the active server main view
            // ("active" at time of task execution because a previous task may have changed the active server main view)
            NetworkManager.CLIENT_NIO_EVENTLOOP.maybeValue?.any { it.inEventLoop() } ?: false
                    || NetworkManager.CLIENT_EPOLL_EVENTLOOP.maybeValue?.any { it.inEventLoop() } ?: false
                    || NetworkManager.CLIENT_LOCAL_EVENTLOOP.maybeValue?.any { it.inEventLoop() } ?: false -> {
                { viewManager.serverMainView }
            }

            // No idea, let's just use the main view
            else -> {
                val exception = Exception()
                // Find the method/class calling Minecraft.addScheduledTask
                val caller = exception.stackTrace
                        //#if MC>=11400
                        //$$ .dropWhile { it.className != ThreadTaskExecutor::class.java.name } // Skip until we're in the mixin
                        //$$ .dropWhile { it.className == ThreadTaskExecutor::class.java.name } // Skip past mixin/enqueue/execute
                        //#else
                        .dropWhile { it.className != Minecraft::class.java.name } // Skip until we're in addScheduledTask
                        .dropWhile { it.className == Minecraft::class.java.name } // Skip past addScheduledTask
                        //#endif
                        .firstOrNull()
                // We know these to be fine with the main view, don't even bother warning
                val confirmedBadCallers = listOf(
                        "net.minecraft.client.resources.SkinManager$3"
                )
                if (!knownBadCallers.contains(caller) && caller?.className !in confirmedBadCallers) {
                    LOGGER.warn("Failed to determine view of task: ", exception)
                    if (caller != null) {
                        LOGGER.warn("Suppressing further warnings for tasks originating from {}", caller)
                        knownBadCallers.add(caller)
                    }
                }
                viewManager.mainView.let { { it } }
            }
        }

        // Return wrapped task
        return ViewWrappedFutureTask(view, inner)
    }

    //#if MC>=11400
    //$$ internal class ViewWrappedFutureTask(
    //$$         private val viewGetter: () -> ClientState?,
    //$$         private val wrapped: Runnable
    //$$ ) : Runnable {
    //$$     override fun run() {
    //$$         val view = viewGetter()
    //$$         if (view == null) {
    //$$             wrapped.run()
    //$$         } else {
    //$$             ClientViewAPIImpl.viewManagerImpl.updateState(view) {
    //$$                 wrapped.run()
    //$$             }
    //$$         }
    //$$     }
    //$$ }
    //#else
    internal class ViewWrappedFutureTask<T>(
            private val viewGetter: () -> ClientState?,
            private val wrapped: FutureTask<T>
    ) : FutureTask<T>({
        val view = viewGetter()
        if (view == null) {
            wrapped.run()
        } else {
            ClientViewAPIImpl.viewManagerImpl.updateState(view) {
                wrapped.run()
            }
        }
        wrapped.get()
    })
    //#endif

    //#if MC<11400
    override fun offer(p0: FutureTask<*>): Boolean = backingQueue.offer(wrapTask(p0))
    override fun iterator(): MutableIterator<FutureTask<*>> = backingQueue.iterator()
    override fun peek(): FutureTask<*>? = backingQueue.peek()
    override fun poll(): FutureTask<*>? = backingQueue.poll()
    override fun isEmpty(): Boolean = backingQueue.isEmpty()
    override val size: Int get() = backingQueue.size
    //#endif
}