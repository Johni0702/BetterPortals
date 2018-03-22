package de.johni0702.minecraft.betterportals.client.view

import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.ListenableFutureTask
import de.johni0702.minecraft.betterportals.BetterPortalsMod
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.BufferBuilder
import net.minecraft.client.renderer.RegionRenderCacheBuilder
import net.minecraft.client.renderer.RenderGlobal
import net.minecraft.client.renderer.chunk.*
import net.minecraft.util.BlockRenderLayer
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.PriorityBlockingQueue
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Almost complete re-implementation of the vanilla [ChunkRenderDispatcher] which can be used by multiple [RenderGlobal]s
 * in a non-conflicting way.
 */
internal class ViewChunkRenderDispatcher : ChunkRenderDispatcher() {
    private val states = ConcurrentHashMap<RenderGlobal, State>()
    private val activeState get() = Minecraft.getMinecraft().renderGlobal.let { states.getOrPut(it, { State(it) }) }

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
                } else {
                    // All queues are empty, wait for new tasks
                    nextChunkUpdateCondition.await()
                }
            }
        }
        throw Error("this should be dead code, kotlin broke")
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

    override fun uploadChunk(layer: BlockRenderLayer, buffer: BufferBuilder, renderChunk: RenderChunk, compiledChunk: CompiledChunk, distSq: Double): ListenableFuture<Any> {
        return if (Minecraft.getMinecraft().isCallingFromMinecraftThread) {
            super.uploadChunk(layer, buffer, renderChunk, compiledChunk, distSq)
        } else {
            activeState.uploadChunk(layer, buffer, renderChunk, compiledChunk, distSq)
        }
    }

    override fun runChunkUploads(finishTimeNano: Long): Boolean = activeState.runChunkUploads(finishTimeNano)
    override fun clearChunkUpdates() { activeState.clearChunkUpdates() }
    override fun stopChunkUpdates() {
        activeState.stopChunkUpdates()
        states.remove(activeState.renderGlobal)
    }

    // Misnamed method, it should actually be "hasNoChunkUpdates"
    override fun hasChunkUpdates(): Boolean =
            states.values.all { it.queuedUpdates.isEmpty() && it.queuedUploads.isEmpty() }

    override fun stopWorkerThreads() {
        val viewManager = BetterPortalsMod.viewManager
        if (viewManager.activeView == viewManager.mainView) {
            states.values.forEach { it.stopChunkUpdates() }
            super.stopWorkerThreads()
        } else {
            stopChunkUpdates()
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

        fun uploadChunk(layer: BlockRenderLayer, buffer: BufferBuilder, renderChunk: RenderChunk, compiledChunk: CompiledChunk, distSq: Double): ListenableFuture<Any> {
            val future: ListenableFutureTask<Any> = ListenableFutureTask.create {
                this@ViewChunkRenderDispatcher.uploadChunk(layer, buffer, renderChunk, compiledChunk, distSq)
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
                runChunkUploads(Long.MAX_VALUE)
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