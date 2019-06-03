package de.johni0702.minecraft.betterportals.common.entity

import de.johni0702.minecraft.betterportals.common.Portal
import de.johni0702.minecraft.betterportals.common.PortalAccessor
import de.johni0702.minecraft.betterportals.common.PortalAgent
import de.johni0702.minecraft.view.server.FixedLocationTicket
import net.minecraft.entity.Entity
import net.minecraft.util.ResourceLocation
import net.minecraft.world.World
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly

interface PortalEntity<out P: Portal.Mutable> {
    val agent: PortalAgent<FixedLocationTicket, P>

    interface OneWay<out P: Portal.Mutable> : PortalEntity<P> {
        /**
         * Whether this portal instance is the tail/exit end of a pair of portals.
         * Not to be confused with the exit portal which spawns after the dragon fight; its tail end is in the overworld.
         * A pair of one-way portals cannot be entered from the tail end.
         */
        val isTailEnd: Boolean

        /**
         * Whether the tail end of the pair of portals is currently visible. Ignored if [isTailEnd] is false.
         * Ignored on the server.
         *
         * The tail end of a one-way portal pair will usually disappear shortly after you've used it.
         */
        val isTailEndVisible: Boolean
    }
}

class PortalEntityAccessor<E, P: Portal.Mutable>(
        private val type: Class<E>,
        private val world: World
) : PortalAccessor<FixedLocationTicket>
        where E: PortalEntity<P>,
              E: Entity
{
    val entities: List<E>
        get() = world.getEntities(type) { it?.isDead == false }
    override val loadedPortals: Iterable<PortalAgent<FixedLocationTicket, Portal.Mutable>>
        get() = entities.map { it.agent }

    override fun findById(id: ResourceLocation): PortalAgent<FixedLocationTicket, Portal.Mutable>? {
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
                      E: PortalEntity<*> = ResourceLocation("minecraft", "entity/id/" + entity.entityId)
    }
}