package de.johni0702.minecraft.betterportals.common

import de.johni0702.minecraft.betterportals.impl.theImpl
import net.minecraft.world.World

/**
 * Entry point into the BetterPortals API.
 */
interface BetterPortalsAPI {
    companion object {
        @JvmStatic
        val instance
            get() = theImpl.portalApi
    }

    fun getPortalManager(world: World): PortalManager
}
