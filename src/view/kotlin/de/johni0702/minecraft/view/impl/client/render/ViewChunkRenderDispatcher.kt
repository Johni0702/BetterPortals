package de.johni0702.minecraft.view.impl.client.render

import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.ListenableFutureTask
import de.johni0702.minecraft.betterportals.common.currentlyOnMainThread
import de.johni0702.minecraft.view.impl.ClientViewAPIImpl
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.BufferBuilder
import net.minecraft.client.renderer.RegionRenderCacheBuilder
import net.minecraft.client.renderer.RenderGlobal
import net.minecraft.client.renderer.chunk.ChunkCompileTaskGenerator
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher
import net.minecraft.client.renderer.chunk.CompiledChunk
import net.minecraft.client.renderer.chunk.RenderChunk
import net.minecraft.util.BlockRenderLayer
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.PriorityBlockingQueue
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock


//#if MC>=11400
//$$ typealias FutureReturnType = Void
//#else
typealias FutureReturnType = Any
//#endif

/**
 * Almost complete re-implementation of the vanilla [ChunkRenderDispatcher] which can be used by multiple [RenderGlobal]s
 * in a non-conflicting way.
 */
internal class ViewChunkRenderDispatcher : ChunkRenderDispatcher(
        //#if MC>=11400
        //$$ Minecraft.getInstance().isJava64bit
        //#endif
) {
    private val states = ConcurrentHashMap<RenderGlobal, State>()
    private val activeState get() = Minecraft.getMinecraft().let { mc ->
        if (!mc.currentlyOnMainThread) {
            // If we're not calling from the MC main thread, then it's probably one of the render workers
            threadAssignment.get()
        } else {
            null
        } ?: mc.renderGlobal.let { states.getOrPut(it, { State(it) }) }
    }

    /**
     * Keeps track of the last state a thread has been associated with.
     * When a thread requests a [RegionRenderCacheBuilder], that allocation is remembered in the last state it has
     * been associated with. That way, if all chunk updates are to be stopped for some state, the main thread
     * can just wait until all of the allocations remembered in that state have been returned.
     */
    private val threadAssignment = ThreadLocal<State>()

    /**
     * Remembers which state a particular [RegionRenderCacheBuilder] has been allocated for and is used when that
     * builder gets freed.
     * @see threadAssignment
     */
    private val renderBuilderAssignment = ConcurrentHashMap<RegionRenderCacheBuilder, State>()

    private val nextChunkUpdateLock = ReentrantLock()
    private val nextChunkUpdateCondition: Condition = nextChunkUpdateLock.newCondition()
    override fun getNextChunkUpdate(): ChunkCompileTaskGenerator {
        // MC is leaking `this` references to other threads in the constructor of ChunkRenderDispatcher, so we need
        // to make sure that this method isn't executed before our own <init> is done.
        @Suppress("SENSELESS_COMPARISON")
        while (nextChunkUpdateCondition == null) {
            Thread.sleep(1000)
        }

        return pollNextChunkUpdate(true)!!
    }

    private fun pollNextChunkUpdate(blocking: Boolean): ChunkCompileTaskGenerator? {
        nextChunkUpdateLock.withLock {
            while (true) {
                // Try to find the next best task in any of the views
                val allHeads = states.values.mapNotNull { state ->
                    state.queuedUpdates.peek()?.let { state to it }
                }
                val minHead = allHeads.minBy { it.second }
                if (minHead != null) {
                    val state = minHead.first
                    // Actual head might have changed since we last checked
                    val actualHead = state.queuedUpdates.poll()
                    if (actualHead != null) {
                        if (Thread.currentThread() in listWorkerThreads) {
                            threadAssignment.set(state)
                        }
                        // Even better head than before
                        return actualHead.also {
                            // Need to notify the next thread which is waiting, that it may check the queues
                            nextChunkUpdateCondition.signal()
                        }
                    } else {
                        // Queue got emptied, try again with the other views
                        continue
                    }
                } else if (blocking) {
                    // All queues are empty, wait for new tasks
                    nextChunkUpdateCondition.await()
                } else {
                    return null
                }
            }
        }
        throw Error("this should be dead code, kotlin broke")
    }

    private fun pollNextChunkUpload(): PendingUpload? {
        while (true) {
            // Try to find the next best task in any of the views
            val allHeads = states.values.mapNotNull { state ->
                state.queuedUploads.peek()?.let { state to it }
            }
            val minHead = allHeads.minBy { it.second }
            return if (minHead != null) {
                minHead.first.queuedUploads.poll() ?: continue
            } else {
                null
            }
        }
    }

    override fun allocateRenderBuilder(): RegionRenderCacheBuilder {
        val state = threadAssignment.get()
        state?.claimedRenderBuilders?.incrementAndGet()
        return super.allocateRenderBuilder().also {
            renderBuilderAssignment[it] = state
        }
    }

    override fun freeRenderBuilder(builder: RegionRenderCacheBuilder) {
        val assignment = renderBuilderAssignment.remove(builder)
        super.freeRenderBuilder(builder)
        assignment?.claimedRenderBuilders?.decrementAndGet()
    }

    override fun updateChunkLater(chunkRenderer: RenderChunk): Boolean =
            activeState.updateChunkLater(chunkRenderer)

    override fun updateTransparencyLater(chunkRenderer: RenderChunk): Boolean =
            activeState.updateTransparencyLater(chunkRenderer)

    override fun uploadChunk(layer: BlockRenderLayer, buffer: BufferBuilder, renderChunk: RenderChunk, compiledChunk: CompiledChunk, distSq: Double): ListenableFuture<FutureReturnType> {
        return if (Minecraft.getMinecraft().currentlyOnMainThread) {
            super.uploadChunk(layer, buffer, renderChunk, compiledChunk, distSq)
        } else {
            activeState.uploadChunk(layer, buffer, renderChunk, compiledChunk, distSq)
        }
    }

    override fun runChunkUploads(finishTimeNano: Long): Boolean = if (ViewRenderPlan.MAIN == ViewRenderPlan.CURRENT) {
        runChunkUploadsForAllViews(finishTimeNano)
    } else {
        activeState.queuedUploads.isNotEmpty()
    }
    override fun clearChunkUpdates() { activeState.clearChunkUpdates() }
    override fun stopChunkUpdates() {
        activeState.stopChunkUpdates()
        states.remove(activeState.renderGlobal)
    }

    // Misnamed method, it should actually be "hasNoChunkUpdates"
    override fun hasChunkUpdates(): Boolean =
            states.values.all { it.queuedUpdates.isEmpty() && it.queuedUploads.isEmpty() }

    override fun stopWorkerThreads() {
        val viewManager = ClientViewAPIImpl.viewManagerImpl
        if (viewManager.activeView == viewManager.mainView) {
            states.values.forEach { it.stopChunkUpdates() }
            super.stopWorkerThreads()
        } else {
            stopChunkUpdates()
        }
    }

    private fun runChunkUploadsForAllViews(finishTimeNano: Long): Boolean {
        var anyUploadDone = false
        do {
            var allDone = true

            if (listWorkerThreads.isEmpty()) {
                val task = pollNextChunkUpdate(false)
                if (task != null) {
                    try {
                        renderWorker.processTask(task)
                        allDone = false
                    } catch (ignored: InterruptedException) {}
                }
            }

            val upload = pollNextChunkUpload()
            if (upload != null) {
                upload.task.run()
                allDone = false
                anyUploadDone = true
            }
        } while (!allDone && finishTimeNano >= System.nanoTime())

        return anyUploadDone
    }

    override fun getDebugInfo(): String {
        val updates = states.values.sumBy { it.queuedUpdates.size }
        return if (listWorkerThreads.isEmpty()) {
            String.format("pN: %1d, pC: %03d, single-threaded", states.size, updates)
        } else {
            val uploads = states.values.sumBy { it.queuedUploads.size }
            val freeBuilders = super.getDebugInfo().splitToSequence(" ").last()
            String.format("pN: %1d, pC: %03d, pU: %1d, aB: %s", states.size, updates, uploads, freeBuilders)
        }
    }

    private inner class State(
            val renderGlobal: RenderGlobal
    ) {
        val claimedRenderBuilders = AtomicInteger(0)
        val queuedUpdates = PriorityBlockingQueue<ChunkCompileTaskGenerator>()
        val queuedUploads = PriorityBlockingQueue<PendingUpload>()

        fun updateChunkLater(chunkRenderer: RenderChunk): Boolean {
            chunkRenderer.lockCompileTask.withLock {
                val task = chunkRenderer.makeCompileTaskChunk()
                task.addFinishRunnable { queuedUpdates.remove(task) }
                return queuedUpdates.offer(task).also { added ->
                    if (added) {
                        nextChunkUpdateLock.withLock {
                            nextChunkUpdateCondition.signal()
                        }
                    } else {
                        task.finish()
                    }
                }
            }
        }
        fun updateTransparencyLater(chunkRenderer: RenderChunk): Boolean {
            chunkRenderer.lockCompileTask.withLock {
                val task = chunkRenderer.makeCompileTaskTransparency() ?: return true
                task.addFinishRunnable { queuedUpdates.remove(task) }
                return queuedUpdates.offer(task).also {
                    nextChunkUpdateLock.withLock {
                        nextChunkUpdateCondition.signal()
                    }
                }
            }
        }

        fun uploadChunk(layer: BlockRenderLayer, buffer: BufferBuilder, renderChunk: RenderChunk, compiledChunk: CompiledChunk, distSq: Double): ListenableFuture<FutureReturnType> {
            val future: ListenableFutureTask<FutureReturnType> = ListenableFutureTask.create {
                this@ViewChunkRenderDispatcher.uploadChunk(layer, buffer, renderChunk, compiledChunk, distSq)
                //#if MC>=11400
                //$$ return@create null
                //#endif
            }

            queuedUploads.add(PendingUpload(future, distSq))
            return future
        }

        fun runChunkUploads(finishTimeNano: Long): Boolean {
            var anyUploadsDone = false
            do {
                var allDone = true

                if (listWorkerThreads.isEmpty()) {
                    val task = queuedUpdates.poll()
                    if (task != null) {
                        try {
                            renderWorker.processTask(task)
                            allDone = false
                        } catch (ignored: InterruptedException) {}
                    }
                }

                if (!queuedUploads.isEmpty()) {
                    queuedUploads.poll()?.task?.run()
                    allDone = false
                    anyUploadsDone = true
                }
            } while (!allDone && finishTimeNano != 0L && finishTimeNano >= System.nanoTime())

            return anyUploadsDone
        }

        fun clearChunkUpdates() {
            while (!queuedUpdates.isEmpty()) {
                queuedUpdates.poll()?.finish()
            }
        }

        fun stopChunkUpdates() {
            clearChunkUpdates()
            while (claimedRenderBuilders.get() > 0) {
                if (!runChunkUploads(Long.MAX_VALUE)) {
                    // No more queued uploads in this state, that either means that:
                    //  a) The worker is still busy producing the upload
                    //  b) The worker is blocked because all renderBuilders are claimed (by upload task in other states)
                    // In the first case we need to wait until it is done (or busy loop, should be relatively quick),
                    // whereas in the second one we need to process upload tasks of other states. If you just do the
                    // latter, we will proceed in both cases.
                    // No need to loop in there though, once we've processed a single upload, a new render builder
                    // should already be available.
                    runChunkUploadsForAllViews(0)
                }
            }
        }
    }

    private class PendingUpload(
            val task: ListenableFutureTask<*>,
            val distSq: Double
    ) : Comparable<PendingUpload> {
        override fun compareTo(other: PendingUpload): Int = distSq.compareTo(other.distSq)

    }
}