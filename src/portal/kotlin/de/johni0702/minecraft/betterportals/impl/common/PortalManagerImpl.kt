package de.johni0702.minecraft.betterportals.impl.common

import de.johni0702.minecraft.betterportals.common.*
import de.johni0702.minecraft.betterportals.impl.net.*
import de.johni0702.minecraft.view.client.worldsManager
import de.johni0702.minecraft.view.server.ServerWorldsManager
import net.minecraft.block.material.Material
import net.minecraft.client.Minecraft
import net.minecraft.entity.Entity
import net.minecraft.util.ResourceLocation
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import net.minecraft.world.WorldServer
import org.apache.logging.log4j.Logger

//#if FABRIC>=1
//$$ import net.fabricmc.fabric.api.event.client.ClientTickCallback
//$$ import net.fabricmc.fabric.api.event.world.WorldTickCallback
//#else
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.world.GetCollisionBoxesEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
//#endif

//#if MC>=11400
//$$ import net.minecraft.util.math.shapes.VoxelShape
//$$ import java.util.stream.Stream
//#else
//#endif

internal interface HasPortalManager {
    val portalManager: PortalManager
}

internal class PortalManagerImpl(override val world: World) : PortalManager {
    override val logger: Logger = LOGGER
    override val preventFallAfterVerticalPortal get() = preventFallDamageGetter()

    private val passiveAccessors = mutableListOf<PortalAccessor>()
    private val activeAccessors = mutableListOf<PortalAccessor>()
    private val accessors = passiveAccessors.asSequence() + activeAccessors.asSequence()

    // The slow ones which need polling
    private val passiveAccessorPortals = passiveAccessors.asSequence().flatMap { it.loadedPortals.asSequence() }
    // The fast ones which will notify us on any changes and therefore allow for caching
    private var activeAccessorPortals = Sequence {
        if (activeAccessorPortalsDirty) {
            activeAccessorPortalsCache.clear()
            activeAccessors.forEach {
                activeAccessorPortalsCache.addAll(it.loadedPortals)
            }
            activeAccessorPortalsDirty = false
        }
        activeAccessorPortalsCache.iterator()
    }
    private var activeAccessorPortalsCache = mutableListOf<PortalAgent<*>>()
    private var activeAccessorPortalsDirty = true

    init {
        EventHandler.registered = true
    }

    override val loadedPortals: Iterable<PortalAgent<*>> = (activeAccessorPortals + passiveAccessorPortals).asIterable()

    override fun registerPortals(accessor: PortalAccessor) {
        if (accessor.onChange { activeAccessorPortalsDirty = true }) {
            activeAccessorPortalsDirty = true
            activeAccessors
        } else {
            passiveAccessors
        }.add(accessor)
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

    override fun serverBeforeUsePortal(agent: PortalAgent<*>, oldEntity: Entity, trackingPlayers: Iterable<ServerWorldsManager>) {
        val packet = EntityUsePortal(EntityUsePortal.Phase.BEFORE, oldEntity.entityId, agent.id).toPacket()
        trackingPlayers.forEach { it.sendPacket(agent.world as WorldServer, packet) }
    }

    override fun serverAfterUsePortal(agent: PortalAgent<*>, newEntity: Entity, trackingPlayers: Iterable<ServerWorldsManager>) {
        val packet = EntityUsePortal(EntityUsePortal.Phase.AFTER, newEntity.entityId, agent.id).toPacket()
        trackingPlayers.forEach { it.sendPacket(agent.world as WorldServer, packet) }
    }

    internal object EventHandler {
        //#if FABRIC>=1
        //$$ var registered = false
        //#else
        var registered by MinecraftForge.EVENT_BUS
        //#endif
        var collisionBoxesEntity by ThreadLocal<Entity>()

        //#if FABRIC>=1
        //$$ init { WorldTickCallback.EVENT.register(WorldTickCallback {
        //$$     if (it is ServerWorld) {
        //$$         tickWorld(it)
        //$$     }
        //$$ }) }
        //#else
        @SubscribeEvent
        fun onWorldTick(event: TickEvent.WorldTickEvent) {
            if (event.phase != TickEvent.Phase.END) return
            if (event.side != LogicalSide.SERVER) return
            tickWorld(event.world)
        }
        //#endif

        //#if FABRIC>=1
        //$$ init { ClientTickCallback.EVENT.register(ClientTickCallback { onClientTick(it) }) }
        //$$ private fun onClientTick(mc: MinecraftClient) {
        //#else
        @SubscribeEvent
        fun onClientTick(event: TickEvent.ClientTickEvent) {
            if (event.phase != TickEvent.Phase.END) return
            val mc = Minecraft.getMinecraft()
        //#endif
            val worldsManager = mc.worldsManager ?: return
            // We need to tick all views to properly update the lastTickPos map.
            // However, actual teleportation will only happen in the main view, since it's
            // the only one containing the player.
            // But worlds will be switched in case there is a teleport, so we map them into a list first.
            worldsManager.worlds.toList().forEach { tickWorld(it) }
        }

        private fun tickWorld(world: World) {
            world.portalManager.loadedPortals.toList().forEach { it.checkTeleportees() }
        }

        //#if MC>=11400
        //$$ // FIXME while the event still exists, it cannot possibly be hooked up properly because it's still using AABBs
        //#else
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
        //#endif

        fun onIsOpenBlockSpace(entity: Entity, pos: BlockPos): Boolean {
            //#if MC>=11400
            //$$ return true // FIXME more or less the same but also not really
            //#else
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
            //#endif
        }

        private fun modifyAABBs(
                entity: Entity,
                entityAABB: AxisAlignedBB,
                queryAABB: AxisAlignedBB,
                //#if MC>=11400
                //$$ vanillaStream: Stream<VoxelShape>,
                //$$ queryRemote: (World, AxisAlignedBB) -> Stream<VoxelShape>
                //#else
                aabbList: MutableList<AxisAlignedBB>,
                queryRemote: (World, AxisAlignedBB) -> List<AxisAlignedBB>
                //#endif
        //#if MC>=11400
        //$$ ): Stream<VoxelShape> {
        //$$     return entity.world.portalManager.loadedPortals.fold(vanillaStream) { stream, agent ->
        //#else
        ) {
            entity.world.portalManager.loadedPortals.forEach { agent ->
        //#endif
                val portal = agent.portal
                if (portal.localBoundingBox.intersects(entityAABB)) { // even remotely?
                    // If this is a non-rectangular portal and the entity isn't inside it, we don't care
                    if (portal.localDetailedBounds.any { it.intersects(entityAABB) }) {
                        //#if MC>=11400
                        //$$ return agent.modifyAABBs(entity, queryAABB, stream, queryRemote)
                        //#else
                        agent.modifyAABBs(entity, queryAABB, aabbList, queryRemote)
                        //#endif
                    }
                }

                //#if MC>=11400
                //$$ stream
                //#endif
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