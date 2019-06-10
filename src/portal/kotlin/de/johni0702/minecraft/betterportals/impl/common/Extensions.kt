package de.johni0702.minecraft.betterportals.impl.common

import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import de.johni0702.minecraft.betterportals.common.server
import de.johni0702.minecraft.betterportals.impl.client.renderer.PortalRenderManager
import de.johni0702.minecraft.betterportals.impl.net.Net
import net.minecraft.client.Minecraft
import net.minecraft.entity.Entity
import net.minecraftforge.common.ForgeChunkManager
import net.minecraftforge.common.capabilities.CapabilityDispatcher
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext
import net.minecraftforge.fml.relauncher.Side
import org.apache.logging.log4j.LogManager

internal const val MOD_ID = "BP/portal"
internal val LOGGER = LogManager.getLogger("betterportals/portal")

internal lateinit var preventFallDamageGetter: () -> Boolean
internal lateinit var maxRenderRecursionGetter: () -> Int

fun initPortal(
        mod: Any,
        init: (() -> Unit) -> Unit,
        clientInit: (() -> Unit) -> Unit,
        preventFallDamage: () -> Boolean,
        maxRenderRecursion: () -> Int
) {
    preventFallDamageGetter = preventFallDamage
    maxRenderRecursionGetter = maxRenderRecursion

    init {
        Net.INSTANCE // initialize via <init>

        // Tickets are only allocated temporarily during remote portal frame search and otherwise aren't needed
        ForgeChunkManager.setForcedChunkLoadingCallback(mod) { tickets, _ ->
            tickets.forEach { ForgeChunkManager.releaseTicket(it) }
        }
    }

    clientInit {
        PortalRenderManager.registered = true
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
