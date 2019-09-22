package de.johni0702.minecraft.betterportals.impl.transition.common

import de.johni0702.minecraft.betterportals.impl.transition.client.renderer.TransferToDimensionRenderer
import de.johni0702.minecraft.betterportals.impl.transition.net.Net
import de.johni0702.minecraft.betterportals.impl.transition.server.DimensionTransitionHandler
import org.apache.logging.log4j.LogManager
import java.time.Duration

internal val LOGGER = LogManager.getLogger("betterportals/transition")

fun initTransition(
        init: (() -> Unit) -> Unit,
        enable: Boolean,
        duration: () -> Int
) {
    DimensionTransitionHandler.enabled = enable
    TransferToDimensionRenderer.defaultDuration = { Duration.ofSeconds(duration().toLong()) }

    init {
        Net.INSTANCE // initialize via <init>
    }
}
