package de.johni0702.minecraft.betterportals.impl.vanilla.common.entity

import de.johni0702.minecraft.betterportals.common.FinitePortal
import de.johni0702.minecraft.betterportals.common.entity.AbstractPortalEntity
import de.johni0702.minecraft.betterportals.impl.vanilla.common.NETHER_PORTAL_CONFIG
import net.minecraft.world.World

//#if MC>=11400
//$$ import net.minecraft.entity.EntityType
//#endif

//#if MC>=11400
//$$ class NetherPortalEntity(type: EntityType<NetherPortalEntity> = ENTITY_TYPE, world: World, portal: FinitePortal) : AbstractPortalEntity(type, world, portal, NETHER_PORTAL_CONFIG) {
//$$     constructor(type: EntityType<NetherPortalEntity> = ENTITY_TYPE, world: World) : this(type, world, FinitePortal.DUMMY)
//$$     companion object {
//$$         lateinit var ENTITY_TYPE: EntityType<NetherPortalEntity>
//$$     }
//$$ }
//#else
class NetherPortalEntity(world: World, portal: FinitePortal) : AbstractPortalEntity(world, portal, NETHER_PORTAL_CONFIG) {
    @Suppress("unused")
    constructor(world: World) : this(world, FinitePortal.DUMMY)
}
//#endif
