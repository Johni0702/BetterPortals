package de.johni0702.minecraft.betterportals.impl.common

import de.johni0702.minecraft.betterportals.impl.TheImpl
import de.johni0702.minecraft.betterportals.impl.client.audio.PortalAwareSoundManager
import de.johni0702.minecraft.betterportals.impl.client.renderer.PortalRenderManager
import de.johni0702.minecraft.betterportals.impl.net.Net
import de.johni0702.minecraft.betterportals.impl.theImpl
import org.apache.logging.log4j.LogManager

//#if MC>=11400
//#else
import net.minecraftforge.common.ForgeChunkManager
//#endif

internal val LOGGER = LogManager.getLogger("betterportals/portal")

internal lateinit var preventFallDamageGetter: () -> Boolean
internal lateinit var maxRenderRecursionGetter: () -> Int

fun initPortal(
        mod: Any,
        init: (() -> Unit) -> Unit,
        clientInit: (() -> Unit) -> Unit,
        preventFallDamage: () -> Boolean,
        dropRemoteSound: () -> Boolean,
        maxRenderRecursion: () -> Int
) {
    theImpl = TheImpl

    preventFallDamageGetter = preventFallDamage
    maxRenderRecursionGetter = maxRenderRecursion

    init {
        Net.INSTANCE // initialize via <init>

        //#if MC<11400
        // Tickets are only allocated temporarily during remote portal frame search and otherwise aren't needed
        ForgeChunkManager.setForcedChunkLoadingCallback(mod) { tickets, _ ->
            tickets.forEach { ForgeChunkManager.releaseTicket(it) }
        }
        //#endif
    }

    clientInit {
        PortalAwareSoundManager.dropRemoteSounds = dropRemoteSound
        PortalRenderManager.registered = true
    }
}

