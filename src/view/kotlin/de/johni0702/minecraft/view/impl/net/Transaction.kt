package de.johni0702.minecraft.view.impl.net

import de.johni0702.minecraft.betterportals.impl.accessors.AccMinecraft
import de.johni0702.minecraft.view.impl.LOGGER
import de.johni0702.minecraft.view.impl.client.ViewDemuxingTaskQueue
import de.johni0702.minecraft.view.impl.common.clientSyncIgnoringView
import net.minecraft.client.Minecraft
import net.minecraft.util.Util
import java.util.concurrent.FutureTask
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit

//#if MC>=11400
//$$ import de.johni0702.minecraft.betterportals.impl.IThreadTaskExecutor
//#endif

internal object Transaction {
    fun lock() {
        if (disableTransactions) return
        // We need to block the network thread until MC's queue has been replaced.
        // Otherwise the network thread might be quick enough to synchronize on the original queue, deadlocking
        val syncedSemaphore = Semaphore(0)
        clientSyncIgnoringView {
            check(!inTransaction) { "Already in transaction!" }
            inTransaction = true
            val mc = Minecraft.getMinecraft()
            mc as AccMinecraft

            //#if MC>=11400
            //$$ // Install the blocking queue which will cause MC to continuously
            //$$ // poll until we un-set it when the transaction ends.
            //$$ IThreadTaskExecutor.from(mc).blockingQueue = LinkedBlockingQueue<Runnable>()
            //$$
            //$$ // MC's queue has been replaced, the networking thread may continue
            //$$ syncedSemaphore.release()
            //#else
            // Replaces MC's queue with a new queue which we aren't currently synchronized on
            val orgQueue = mc.scheduledTasks
            val backingQueue = LinkedBlockingQueue<FutureTask<*>>()
            val tmpQueue = ViewDemuxingTaskQueue(mc, backingQueue)
            // Drain the original queue into our new one before replacing, to keep task order
            tmpQueue.addAll(orgQueue)
            mc.scheduledTasks = tmpQueue

            // MC's queue has been replaced, the networking thread may continue
            syncedSemaphore.release()

            // Block main loop / run tasks while in transaction
            while (inTransaction) {
                Util.runTask(backingQueue.poll(Long.MAX_VALUE, TimeUnit.NANOSECONDS), LOGGER)
            }

            // Restore original queue
            synchronized(mc.scheduledTasks) {
                orgQueue.addAll(mc.scheduledTasks)
                mc.scheduledTasks = orgQueue
            }
            //#endif
        }
        // Wait for main thread to be ready
        syncedSemaphore.acquire()
    }

    fun unlock() {
        if (disableTransactions) return
        clientSyncIgnoringView {
            check(inTransaction) { "Transaction end without start!" }
            inTransaction = false
            //#if MC>=11400
            //$$ IThreadTaskExecutor.from(Minecraft.getInstance()).blockingQueue = null
            //#endif
        }
    }

    private var inTransaction = false

    /**
     * During single-threaded testing, the transaction handler results in dead locks and is useless anyway.
     * Warning: During normal operation, transactions are required and disabling them will result in bugs!
     */
    var disableTransactions = false
}