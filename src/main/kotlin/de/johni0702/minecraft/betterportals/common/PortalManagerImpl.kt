package de.johni0702.minecraft.betterportals.common

import de.johni0702.minecraft.betterportals.BPConfig
import de.johni0702.minecraft.betterportals.LOGGER
import de.johni0702.minecraft.betterportals.net.*
import de.johni0702.minecraft.view.server.CanMakeMainView
import net.minecraft.block.material.Material
import net.minecraft.client.Minecraft
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.util.ResourceLocation
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.world.GetCollisionBoxesEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import net.minecraftforge.fml.relauncher.Side
import org.apache.logging.log4j.Logger

interface HasPortalManager {
    val portalManager: PortalManager
}

class PortalManagerImpl(override val world: World) : PortalManager {
    override val logger: Logger = LOGGER
    override val preventFallAfterVerticalPortal get() = BPConfig.preventFallDamage
    private val accessors = mutableListOf<PortalAccessor<*>>()

    init {
        EventHandler.registered = true
    }

    override val loadedPortals: Iterable<PortalAgent<*, *>>
        get() = accessors.flatMap { it.loadedPortals }

    override fun registerPortals(accessor: PortalAccessor<*>) {
        accessors.add(accessor)
    }

    override fun findById(id: ResourceLocation): PortalAgent<*, *>? {
        for (accessor in accessors) {
            return accessor.findById(id) ?: continue
        }
        return null
    }

    override fun clientUsePortal(agent: PortalAgent<*, *>) {
        Net.INSTANCE.sendToServer(UsePortal(agent.id))
    }

    override fun serverBeforeUsePortal(agent: PortalAgent<*, *>, oldEntity: Entity, trackingPlayers: Iterable<EntityPlayerMP>) {
        EntityUsePortal(EntityUsePortal.Phase.BEFORE, oldEntity.entityId, agent.id).sendTo(trackingPlayers)
    }

    override fun serverAfterUsePortal(agent: PortalAgent<*, *>, newEntity: Entity, trackingPlayers: Iterable<EntityPlayerMP>) {
        EntityUsePortal(EntityUsePortal.Phase.AFTER, newEntity.entityId, agent.id).sendTo(trackingPlayers)
    }

    override fun linkPortal(agent: PortalAgent<*, *>, player: EntityPlayerMP, ticket: CanMakeMainView) {
        val view = ticket.view
        LinkPortal(
                agent.id,
                agent.portal.writePortalToNBT(),
                view.id
        ).sendTo(player)
    }

    override fun unlinkPortal(agent: PortalAgent<*, *>, player: EntityPlayerMP) {
        LinkPortal(
                agent.id,
                agent.portal.writePortalToNBT(),
                null
        ).sendTo(player)
    }

    internal object EventHandler {
        var registered by MinecraftForge.EVENT_BUS
        var collisionBoxesEntity: Entity? = null

        @SubscribeEvent
        fun onWorldTick(event: TickEvent.WorldTickEvent) {
            if (event.phase != TickEvent.Phase.END) return
            if (event.side != Side.SERVER) return
            tickWorld(event.world)
        }

        @SubscribeEvent
        fun onClientTick(event: TickEvent.ClientTickEvent) {
            if (event.phase != TickEvent.Phase.END) return
            val world = Minecraft.getMinecraft().world
            if (world != null) {
                tickWorld(world)
            }
        }

        private fun tickWorld(world: World) {
            world.portalManager.loadedPortals.forEach { it.checkTeleportees() }
        }

        @SubscribeEvent(priority = EventPriority.LOW)
        fun onGetCollisionBoxes(event: GetCollisionBoxesEvent) {
            val entity = event.entity ?: collisionBoxesEntity ?: return
            modifyAABBs(entity, event.aabb, event.aabb, event.collisionBoxesList) { world, aabb ->
                val collisionBoxesEntity = this.collisionBoxesEntity
                this.collisionBoxesEntity = null
                try {
                    world.getCollisionBoxes(null, aabb)
                } finally {
                    this.collisionBoxesEntity = collisionBoxesEntity
                }
            }
        }

        fun onIsOpenBlockSpace(entity: Entity, pos: BlockPos): Boolean {
            val query = { world: World, aabb: AxisAlignedBB ->
                val blockPos = aabb.min.toBlockPos()
                val blockState = world.getBlockState(blockPos)
                if (blockState.block.isNormalCube(blockState, world, blockPos)) {
                    mutableListOf(AxisAlignedBB(blockPos))
                } else {
                    mutableListOf()
                }
            }
            val aabbList = query(entity.world, AxisAlignedBB(pos))
            modifyAABBs(entity, entity.entityBoundingBox, AxisAlignedBB(pos), aabbList, query)
            return aabbList.isEmpty()
        }

        private fun modifyAABBs(
                entity: Entity,
                entityAABB: AxisAlignedBB,
                queryAABB: AxisAlignedBB,
                aabbList: MutableList<AxisAlignedBB>,
                queryRemote: (World, AxisAlignedBB) -> List<AxisAlignedBB>
        ) {
            entity.world.portalManager.loadedPortals.forEach { agent ->
                val portal = agent.portal
                if (!portal.localBoundingBox.intersects(entityAABB)) return@forEach // not even close
                // If this is a non-rectangular portal and the entity isn't inside it, we don't care
                if (portal.localDetailedBounds.none { it.intersects(entityAABB) }) return@forEach

                agent.modifyAABBs(entity, queryAABB, aabbList, queryRemote)
            }
        }

        fun isInMaterial(entity: Entity, material: Material): Boolean? {
            val entityAABB = entity.entityBoundingBox
            val queryAABB = entityAABB.grow(-0.1, -0.4, -0.1)

            entity.world.portalManager.loadedPortals.forEach { agent ->
                val portal = agent.portal
                if (!portal.localBoundingBox.intersects(entityAABB)) return@forEach // not even close
                // If this is a non-rectangular portal and the entity isn't inside it, we don't care
                if (portal.localDetailedBounds.none { it.intersects(entityAABB) }) return@forEach

                return agent.isInMaterial(entity, queryAABB, material) ?: return@forEach
            }

            // Entity not in any portal, fallback to default implementation
            return null
        }
    }

}