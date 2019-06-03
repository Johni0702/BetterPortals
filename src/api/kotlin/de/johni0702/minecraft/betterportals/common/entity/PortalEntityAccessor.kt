package de.johni0702.minecraft.betterportals.common.entity

import de.johni0702.minecraft.betterportals.common.PortalAccessor
import de.johni0702.minecraft.betterportals.common.PortalAgent
import de.johni0702.minecraft.view.server.FixedLocationTicket
import net.minecraft.entity.Entity
import net.minecraft.util.ResourceLocation
import net.minecraft.world.World

interface PortalEntity {
    val agent: PortalAgent<FixedLocationTicket>
}

class PortalEntityAccessor<E>(
        private val type: Class<E>,
        private val world: World
) : PortalAccessor<FixedLocationTicket>
        where E: PortalEntity,
              E: Entity
{
    val entities: List<E>
        get() = world.getEntities(type) { it?.isDead == false }
    override val loadedPortals: Iterable<PortalAgent<FixedLocationTicket>>
        get() = entities.map { it.agent }

    override fun findById(id: ResourceLocation): PortalAgent<FixedLocationTicket>? {
        if (id.resourceDomain != "minecraft") return null
        if (!id.resourcePath.startsWith("entity/id/")) return null
        val entityId = id.resourcePath.substring("entity/id/".length).toIntOrNull() ?: return null
        val entity = world.getEntityByID(entityId)
        if (!type.isInstance(entity)) return null
        return type.cast(world.getEntityByID(entityId))?.agent
    }

    companion object {
        fun <E> getId(entity: E): ResourceLocation
                where E: Entity,
                      E: PortalEntity = ResourceLocation("minecraft", "entity/id/" + entity.entityId)
    }
}