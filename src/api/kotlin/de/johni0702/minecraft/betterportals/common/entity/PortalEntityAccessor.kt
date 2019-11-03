package de.johni0702.minecraft.betterportals.common.entity

import de.johni0702.minecraft.betterportals.common.FinitePortal
import de.johni0702.minecraft.betterportals.common.PortalAccessor
import de.johni0702.minecraft.betterportals.common.PortalAgent
import de.johni0702.minecraft.betterportals.impl.theImpl
import net.minecraft.entity.Entity
import net.minecraft.util.ResourceLocation
import net.minecraft.world.World

//#if MC>=11400
//$$ import de.johni0702.minecraft.betterportals.impl.accessors.AccServerWorld
//$$ import java.lang.UnsupportedOperationException
//$$ import net.minecraft.client.world.ClientWorld
//$$ import net.minecraft.world.server.ServerWorld
//#endif

interface PortalEntity {
    val agent: PortalAgent<FinitePortal>

    interface OneWay : PortalEntity {
        /**
         * Whether this portal instance is the tail/exit end of a pair of portals.
         * Not to be confused with the exit portal which spawns after the dragon fight; its tail end is in the overworld.
         * A pair of one-way portals cannot be entered from the tail end.
         */
        val isTailEnd: Boolean

        /**
         * Whether the tail end of the pair of portals is currently visible. Ignored if [isTailEnd] is false.
         *
         * The tail end of a one-way portal pair will usually disappear shortly after you've used it.
         */
        val isTailEndVisible: Boolean
    }
}

class PortalEntityAccessor<E>(
        private val type: Class<E>,
        private val world: World
) : PortalAccessor
        where E: PortalEntity,
              E: Entity
{
    val entities: List<E>
        //#if MC>=11400
        //$$ get() = when (world) {
        //$$     is ServerWorld -> (world as AccServerWorld).entitiesById.values.filterIsInstanceTo(mutableListOf(), type)
        //$$     is ClientWorld -> world.allEntities.filterIsInstanceTo(mutableListOf(), type)
        //$$     else -> throw UnsupportedOperationException()
        //$$ }
        //#else
        get() = world.getEntities(type) { it?.isDead == false }
        //#endif
    override val loadedPortals: Iterable<PortalAgent<FinitePortal>>
        get() = entities.map { it.agent }

    private val changeCallbacks = mutableListOf<() -> Unit>()
    init {
        with(theImpl) {
            world.addEntitiesListener(onEntityAdded = { entity ->
                if (type.isInstance(entity)) {
                    changeCallbacks.forEach { it() }
                }
            }, onEntityRemoved = { entity ->
                if (type.isInstance(entity)) {
                    changeCallbacks.forEach { it() }
                }
            })
        }
    }

    override fun findById(id: ResourceLocation): PortalAgent<FinitePortal>? {
        if (id.resourceDomain != "minecraft") return null
        if (!id.resourcePath.startsWith("entity/id/")) return null
        val entityId = id.resourcePath.substring("entity/id/".length).toIntOrNull() ?: return null
        val entity = world.getEntityByID(entityId)
        if (!type.isInstance(entity)) return null
        return type.cast(world.getEntityByID(entityId))?.agent
    }

    override fun onChange(callback: () -> Unit): Boolean {
        changeCallbacks.add(callback)
        return true
    }

    companion object {
        fun <E> getId(entity: E): ResourceLocation
                where E: Entity,
                      E: PortalEntity = ResourceLocation("minecraft", "entity/id/" + entity.entityId)
    }
}