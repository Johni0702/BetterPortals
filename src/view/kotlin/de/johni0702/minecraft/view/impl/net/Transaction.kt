package de.johni0702.minecraft.view.impl.net

import de.johni0702.minecraft.betterportals.common.readEnum
import de.johni0702.minecraft.betterportals.common.writeEnum
import de.johni0702.minecraft.view.impl.LOGGER
import de.johni0702.minecraft.view.impl.client.ViewDemuxingTaskQueue
import de.johni0702.minecraft.view.impl.common.sync
import io.netty.buffer.ByteBuf
import net.minecraft.client.Minecraft
import net.minecraft.network.PacketBuffer
import net.minecraft.util.Util
import net.minecraftforge.fml.common.network.simpleimpl.IMessage
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext
import java.util.concurrent.FutureTask
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit

internal class Transaction(
        var phase: Phase = Phase.START
) : IMessage {
    override fun fromBytes(buf: ByteBuf) {
        with(PacketBuffer(buf)) {
            phase = readEnum()
        }
    }

    override fun toBytes(buf: ByteBuf) {
        with(PacketBuffer(buf)) {
            writeEnum(phase)
        }
    }

    internal class Handler : IMessageHandler<Transaction, IMessage> {
        override fun onMessage(message: Transaction, ctx: MessageContext): IMessage? {
            when(message.phase) {
                Phase.START -> {
                    // We need to block the network thread until MC's queue has been replaced.
                    // Otherwise the network thread might be quick enough to synchronize on the original queue, deadlocking
                    val syncedSemaphore = Semaphore(0)
                    ctx.sync {
                        if (inTransaction++ == 0) {
                            val mc = Minecraft.getMinecraft()

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
                            while (inTransaction > 0) {
                                Util.runTask(backingQueue.poll(Long.MAX_VALUE, TimeUnit.NANOSECONDS), LOGGER)
                            }

                            // Restore original queue
                            synchronized(mc.scheduledTasks) {
                                orgQueue.addAll(mc.scheduledTasks)
                                mc.scheduledTasks = orgQueue
                            }
                        } else {
                            syncedSemaphore.release()
                        }
                    }
                    // Wait for main thread to be ready
                    syncedSemaphore.acquire()
                }
                Phase.END -> {
                    ctx.sync {
                        if (inTransaction <= 0) throw IllegalStateException("Transaction end without start!")
                        inTransaction--
                    }
                }
            }
            return null
        }
    }

    enum class Phase {
        START, END
    }

    companion object {
        private var inTransaction = 0
    }
}