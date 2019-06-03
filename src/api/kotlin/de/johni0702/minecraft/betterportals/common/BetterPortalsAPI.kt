package de.johni0702.minecraft.betterportals.common

import net.minecraft.world.World
import net.minecraftforge.fml.common.Loader

/**
 * Entry point into the BetterPortals API.
 */
interface BetterPortalsAPI {
    companion object {
        @JvmStatic
        val instance by lazy { Loader.instance().indexedModList["betterportals"]!!.mod as BetterPortalsAPI }
    }

    fun getPortalManager(world: World): PortalManager
}
