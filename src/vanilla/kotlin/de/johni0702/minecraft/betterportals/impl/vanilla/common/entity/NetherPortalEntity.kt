package de.johni0702.minecraft.betterportals.impl.vanilla.common.entity

import de.johni0702.minecraft.betterportals.common.FinitePortal
import de.johni0702.minecraft.betterportals.common.entity.AbstractPortalEntity
import de.johni0702.minecraft.betterportals.impl.vanilla.common.NETHER_PORTAL_CONFIG
import net.minecraft.world.World

//#if MC>=11400
//$$ import de.johni0702.minecraft.betterportals.common.ObjectHolder
//$$ import de.johni0702.minecraft.betterportals.impl.vanilla.common.MOD_ID
//$$ import net.minecraft.util.ResourceLocation
//$$ import net.minecraft.entity.EntityType
//$$ import net.minecraftforge.registries.ForgeRegistries
//#endif

//#if MC>=11400
//$$ class NetherPortalEntity(type: EntityType<NetherPortalEntity> = ENTITY_TYPE, world: World, portal: FinitePortal) : AbstractPortalEntity(type, world, portal, NETHER_PORTAL_CONFIG) {
//$$     constructor(type: EntityType<NetherPortalEntity> = ENTITY_TYPE, world: World) : this(type, world, FinitePortal.DUMMY)
//$$     companion object {
//$$         val ID = ResourceLocation("$MOD_ID:nether_portal")
//$$         val ENTITY_TYPE: EntityType<NetherPortalEntity> by ObjectHolder(ForgeRegistries.ENTITIES, ID)
//$$     }
//$$ }
//#else
class NetherPortalEntity(world: World, portal: FinitePortal) : AbstractPortalEntity(world, portal, NETHER_PORTAL_CONFIG) {
    @Suppress("unused")
    constructor(world: World) : this(world, FinitePortal.DUMMY)
}
//#endif
