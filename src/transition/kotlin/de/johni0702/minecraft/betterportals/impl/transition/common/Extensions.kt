package de.johni0702.minecraft.betterportals.impl.transition.common

import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import de.johni0702.minecraft.betterportals.common.server
import de.johni0702.minecraft.betterportals.impl.transition.net.Net
import de.johni0702.minecraft.betterportals.impl.transition.server.DimensionTransitionHandler
import net.minecraft.client.Minecraft
import net.minecraft.entity.Entity
import net.minecraftforge.common.capabilities.CapabilityDispatcher
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext
import net.minecraftforge.fml.relauncher.Side
import org.apache.logging.log4j.LogManager

internal const val MOD_ID = "BP/transition"
internal val LOGGER = LogManager.getLogger("betterportals/transition")

fun initTransition(
        init: (() -> Unit) -> Unit,
        enable: Boolean
) {
    DimensionTransitionHandler.enabled = enable

    init {
        Net.INSTANCE // initialize via <init>
    }
}

internal fun MessageContext.sync(task: () -> Unit) = when(side!!) {
    Side.CLIENT -> syncOnClient(task)
    Side.SERVER -> syncOnServer(task)
}
// Note: must be in separate method so we can access client-only methods/classes
private fun syncOnClient(task: () -> Unit) = Minecraft.getMinecraft().addScheduledTask(task).logFailure()
private fun MessageContext.syncOnServer(task: () -> Unit) = serverHandler.player.serverWorld.server.addScheduledTask(task).logFailure()
internal fun <L : ListenableFuture<T>, T> L.logFailure(): L {
    Futures.addCallback(this, object : FutureCallback<T> {
        override fun onSuccess(result: T?) = Unit
        override fun onFailure(t: Throwable) {
            LOGGER.error("Failed future:", t)
        }
    })
    return this
}

private val forgeCapabilitiesField = Entity::class.java.getDeclaredField("capabilities").apply { isAccessible = true }
internal var Entity.forgeCapabilities: CapabilityDispatcher?
    get() = forgeCapabilitiesField.get(this) as CapabilityDispatcher?
    set(value) = forgeCapabilitiesField.set(this, value)
