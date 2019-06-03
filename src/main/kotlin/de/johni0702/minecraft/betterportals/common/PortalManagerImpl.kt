package de.johni0702.minecraft.betterportals.common

import de.johni0702.minecraft.betterportals.BPConfig
import de.johni0702.minecraft.betterportals.LOGGER
import de.johni0702.minecraft.betterportals.net.*
import de.johni0702.minecraft.view.server.CanMakeMainView
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.util.ResourceLocation
import net.minecraft.world.World
import org.apache.logging.log4j.Logger

interface HasPortalManager {
    val portalManager: PortalManager
}

class PortalManagerImpl(override val world: World) : PortalManager {
    override val logger: Logger = LOGGER
    override val preventFallAfterVerticalPortal get() = BPConfig.preventFallDamage
    private val accessors = mutableListOf<PortalAccessor<*>>()

    override val loadedPortals: Iterable<PortalAgent<*>>
        get() = accessors.flatMap { it.loadedPortals }

    override fun registerPortals(accessor: PortalAccessor<*>) {
        accessors.add(accessor)
    }

    override fun findById(id: ResourceLocation): PortalAgent<*>? {
        for (accessor in accessors) {
            return accessor.findById(id) ?: continue
        }
        return null
    }

    override fun clientUsePortal(agent: PortalAgent<*>) {
        Net.INSTANCE.sendToServer(UsePortal(agent.id))
    }

    override fun serverBeforeUsePortal(agent: PortalAgent<*>, oldEntity: Entity, trackingPlayers: Iterable<EntityPlayerMP>) {
        EntityUsePortal(EntityUsePortal.Phase.BEFORE, oldEntity.entityId, agent.id).sendTo(trackingPlayers)
    }

    override fun serverAfterUsePortal(agent: PortalAgent<*>, newEntity: Entity, trackingPlayers: Iterable<EntityPlayerMP>) {
        EntityUsePortal(EntityUsePortal.Phase.AFTER, newEntity.entityId, agent.id).sendTo(trackingPlayers)
    }

    override fun linkPortal(agent: PortalAgent<*>, player: EntityPlayerMP, ticket: CanMakeMainView) {
        val view = ticket.view
        LinkPortal(
                agent.id,
                agent.portal.writePortalToNBT(),
                view.id
        ).sendTo(player)
    }

    override fun unlinkPortal(agent: PortalAgent<*>, player: EntityPlayerMP) {
        LinkPortal(
                agent.id,
                agent.portal.writePortalToNBT(),
                null
        ).sendTo(player)
    }
}