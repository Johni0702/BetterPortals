package de.johni0702.minecraft.betterportals.impl.vanilla.common.entity

import de.johni0702.minecraft.betterportals.common.FinitePortal
import de.johni0702.minecraft.betterportals.common.entity.AbstractPortalEntity
import de.johni0702.minecraft.betterportals.impl.vanilla.common.NETHER_PORTAL_CONFIG
import net.minecraft.world.World

class NetherPortalEntity(world: World, portal: FinitePortal) : AbstractPortalEntity(world, portal, NETHER_PORTAL_CONFIG) {
    @Suppress("unused")
    constructor(world: World) : this(world, FinitePortal.DUMMY)
}