package de.johni0702.minecraft.betterportals.impl

import de.johni0702.minecraft.betterportals.common.BetterPortalsAPI
import de.johni0702.minecraft.betterportals.common.PortalManager
import de.johni0702.minecraft.betterportals.impl.common.HasPortalManager
import net.minecraft.world.World

object BetterPortalsAPIImpl : BetterPortalsAPI {
    override fun getPortalManager(world: World): PortalManager = (world as HasPortalManager).portalManager
}