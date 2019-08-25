package de.johni0702.minecraft.betterportals.impl.abyssalcraft.common.entity

import de.johni0702.minecraft.betterportals.common.FinitePortal
import de.johni0702.minecraft.betterportals.common.entity.AbstractPortalEntity
import de.johni0702.minecraft.betterportals.impl.abyssalcraft.common.ABYSSALCRAFT_PORTAL_CONFIG
import net.minecraft.world.World

class DreadlandsPortalEntity(
        world: World,
        portal: FinitePortal
) : AbstractPortalEntity(
        world,
        portal,
        ABYSSALCRAFT_PORTAL_CONFIG
) {
    @Suppress("unused")
    constructor(world: World) : this(world, FinitePortal.DUMMY)
}