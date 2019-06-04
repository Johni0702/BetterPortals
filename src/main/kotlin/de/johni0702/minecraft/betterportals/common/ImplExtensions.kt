package de.johni0702.minecraft.betterportals.common

import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import de.johni0702.minecraft.betterportals.BetterPortalsMod
import de.johni0702.minecraft.betterportals.LOGGER
import net.minecraft.entity.Entity
import net.minecraft.network.NetHandlerPlayServer
import net.minecraft.util.LazyLoadBase
import net.minecraft.world.World
import net.minecraftforge.common.capabilities.CapabilityDispatcher
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext

fun World?.sync(task: () -> Unit) = BetterPortalsMod.PROXY.sync(this, task)
fun MessageContext.sync(task: () -> Unit) = (this.netHandler as? NetHandlerPlayServer)?.player?.world.sync(task)
fun <L : ListenableFuture<T>, T> L.logFailure(): L {
    Futures.addCallback(this, object : FutureCallback<T> {
        override fun onSuccess(result: T?) = Unit
        override fun onFailure(t: Throwable) {
            LOGGER.error("Failed future:", t)
        }
    })
    return this
}

val <T> LazyLoadBase<T>.maybeValue get() = if (isLoaded) value else null

private val forgeCapabilitiesField = Entity::class.java.getDeclaredField("capabilities").apply { isAccessible = true }
var Entity.forgeCapabilities: CapabilityDispatcher
    get() = forgeCapabilitiesField.get(this) as CapabilityDispatcher
    set(value) = forgeCapabilitiesField.set(this, value)
