//#if MC<11400
package de.johni0702.minecraft.betterportals.impl.abyssalcraft.common.entity

import de.johni0702.minecraft.betterportals.common.FinitePortal
import de.johni0702.minecraft.betterportals.common.entity.AbstractPortalEntity
import de.johni0702.minecraft.betterportals.impl.abyssalcraft.BPAbyssalcraftMod.Companion.PORTAL_CONFIG
import net.minecraft.world.World

class DreadlandsPortalEntity(
        world: World,
        portal: FinitePortal
) : AbstractPortalEntity(
        world,
        portal,
        PORTAL_CONFIG
) {
    @Suppress("unused")
    constructor(world: World) : this(world, FinitePortal.DUMMY)
}
//#endif