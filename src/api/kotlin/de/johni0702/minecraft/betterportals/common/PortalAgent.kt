package de.johni0702.minecraft.betterportals.common

import de.johni0702.minecraft.betterportals.client.deriveClientPosRotFrom
import de.johni0702.minecraft.view.client.ClientView
import de.johni0702.minecraft.view.server.*
import net.minecraft.block.material.Material
import net.minecraft.client.entity.AbstractClientPlayer
import net.minecraft.client.entity.EntityOtherPlayerMP
import net.minecraft.client.entity.EntityPlayerSP
import net.minecraft.client.renderer.culling.ICamera
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityList
import net.minecraft.entity.item.EntityMinecart
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.network.play.server.SPacketSetPassengers
import net.minecraft.util.EnumFacing
import net.minecraft.util.ResourceLocation
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.Vec3d
import net.minecraft.world.World
import net.minecraft.world.WorldServer
import net.minecraftforge.common.ForgeHooks
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly
import org.apache.logging.log4j.Logger
import java.lang.IllegalArgumentException

interface PortalAccessor<T: CanMakeMainView> {
    /**
     * Collection of all, currently loaded portals known to this accessor.
     *
     * When implementing, take note of [onChange].
     */
    val loadedPortals: Iterable<PortalAgent<T, *>>

    /**
     * Retrieve an agent by id. See [PortalAgent.id].
     */
    fun findById(id: ResourceLocation): PortalAgent<T, *>?

    /**
     * The [loadedPortals] field (or rather [PortalManager.loadedPortals]) needs to be accessed **very** often (e.g.
     * to determine collision boxes or to check if an entity touches lava) while its content changes relatively
     * infrequently (e.g. user builds/destroys portal or chunk with portal loads/unloads).
     *
     * To improve performance, this method should be implemented.
     * Where this is not possible
     *
     * If implemented, it must return `true` and arrange for the `callback` to be invoked whenever [loadedPortals]
     * changes. The [PortalManager] will then only have to update its cache whenever its callback is invoked.
     */
    fun onChange(callback: () -> Unit): Boolean = false
}

interface PortalManager {
    val logger: Logger
    val world: World

    /**
     * Collection of all, currently loaded portals.
     *
     * This method must perform well (both CPU and garbage wise) as it will be called **very** often.
     */
    val loadedPortals: Iterable<PortalAgent<*, *>>

    /**
     * Registers a new source for [loadedPortals].
     */
    fun registerPortals(accessor: PortalAccessor<*>)

    /**
     * Retrieve an agent by id. See [PortalAccessor.findById].
     */
    fun findById(id: ResourceLocation): PortalAgent<*, *>?

    /**
     * Called immediately after the client player uses a portal to inform the server of that action.
     * Calls [PortalAgent.serverPortalUsed] on the corresponding agent on the server.
     *
     * Passing an agent which is not in [loadedPortals] is an error.
     */
    @SideOnly(Side.CLIENT)
    fun clientUsePortal(agent: PortalAgent<*, *>)

    /**
     * Called right before an entity uses a portal to ensure clients are prepared for it.
     * Calls [PortalAgent.beforeUsePortal] on the corresponding agent on the client.
     *
     * Passing an agent which is not in [loadedPortals] is an error.
     */
    fun serverBeforeUsePortal(agent: PortalAgent<*, *>, oldEntity: Entity, trackingPlayers: Iterable<EntityPlayerMP>)

    /**
     * Called right after an entity uses a portal to allow clients to properly display the transition.
     * Calls [PortalAgent.afterUsePortal] on the corresponding agent on the client.
     *
     * Passing an agent which is not in [loadedPortals] is an error.
     */
    fun serverAfterUsePortal(agent: PortalAgent<*, *>, newEntity: Entity, trackingPlayers: Iterable<EntityPlayerMP>)

    /**
     * Link the given portal agent to the given view on the client.
     */
    fun linkPortal(agent: PortalAgent<*, *>, player: EntityPlayerMP, ticket: CanMakeMainView)

    /**
     * Unlink the given portal agent on the client.
     */
    fun unlinkPortal(agent: PortalAgent<*, *>, player: EntityPlayerMP)

    /**
     * Whether to (by default) prevent the next fall damage after passing through a vertical portal.
     */
    val preventFallAfterVerticalPortal: Boolean
}

val World.portalManager get() = BetterPortalsAPI.instance.getPortalManager(this)

open class PortalAgent<T: CanMakeMainView, out P: Portal.Mutable>(
        val manager: PortalManager,
        /**
         * A unique (per world/manager) id for this agent.
         * The id must be shared with the corresponding agent on the client/server side and at least one portal accessor
         * registered must be able to resolve it via [PortalAccessor.findById] on either side.
         */
        open val id: ResourceLocation,
        val portal: P,
        val allocateTicket: (ServerView) -> T?,
        val portalConfig: PortalConfiguration
) {
    @Deprecated("missing `PortalConfig` argument")
    constructor(manager: PortalManager, id: ResourceLocation, portal: P, allocateTicket: (ServerView) -> T?)
            : this(manager, id, portal, allocateTicket, PortalConfiguration())

    val world get() = manager.world
    open val preventFallAfterVerticalPortal get() = manager.preventFallAfterVerticalPortal

    open fun isLinked(other: PortalAgent<*, *>): Boolean =
            other.portal.isTarget(portal) && portal.isTarget(other.portal)

    open fun getRemoteAgent(): PortalAgent<T, P>? {
        val remoteWorld = if (world.isRemote) {
            (view ?: return null).world
        } else {
            world.minecraftServer!!.getWorld(portal.remoteDimension ?: return null)
        }
        remoteWorld.getBlockState(portal.remotePosition) // make sure the portal is loaded
        val remotePortal = remoteWorld.portalManager.loadedPortals.find { isLinked(it) }
        @Suppress("UNCHECKED_CAST")
        return remotePortal as PortalAgent<T, P>?
    }

    private var lastTickPos = mutableMapOf<Entity, Vec3d>()

    /**
     * Returns the side of the portal on which the entity resides.
     * This method takes into account that even though the entity might have already passed through the portal surface
     * it might have not yet been teleported. In such cases, this method returns the logical side (i.e. the one on
     * which the entity entered the portal), not the actual side one would get by naively comparing coordinates.
     */
    fun getEntitySide(entity: Entity): EnumFacing {
        val riddenEntity = entity.lowestRidingEntity
        val entityPos = lastTickPos[riddenEntity] ?: (riddenEntity.pos + riddenEntity.eyeOffset)
        return portal.localFacing.axis.toFacing(entityPos - portal.localPosition.to3dMid())
    }

    /**
     * Called to compute collision boxes for the given entity in and around this portal (especially for adding those
     * in the remote dimension, which vanilla does not know about, and removing those on the hidden side, which the
     * entity should not interact with).
     * Only those BBs which intersect with `queryAABB` need to be added but *any* incorrect ones should be removed.
     *
     * This method is only called for selected portals (those which intersect with either entity BB or queryAABB).
     */
    open fun modifyAABBs(
            entity: Entity,
            queryAABB: AxisAlignedBB,
            aabbList: MutableList<AxisAlignedBB>,
            queryRemote: (World, AxisAlignedBB) -> List<AxisAlignedBB>
    ) {
        val remotePortal = getRemoteAgent()
        if (remotePortal == null) {
            // Remote portal hasn't yet been loaded, treat all portal blocks as solid to prevent passing
            portal.localDetailedBounds.forEach {
                if (it.intersects(queryAABB)) {
                    aabbList.add(it)
                }
            }
            return
        }

        // otherwise, we need to remove all collision boxes on the other, local side of the portal
        // to prevent the entity from colliding with them
        val entitySide = getEntitySide(entity)
        val hiddenSide = entitySide.opposite
        val hiddenAABB = portal.localBoundingBox
                .offset(hiddenSide.directionVec.to3d())
                .expand(hiddenSide.directionVec.to3d() * Double.POSITIVE_INFINITY)
        aabbList.removeIf { it.intersects(hiddenAABB) }

        // and instead add collision boxes from the remote world
        if (!hiddenAABB.intersects(queryAABB)) return // unless we're not even interested in those
        val remoteAABB = with(portal) {
            // Reduce the AABB which we're looking for in the first place to the hidden section
            val aabb = hiddenAABB.intersect(queryAABB)
            // and transform it to remote space in order to lookup collision boxes over there
            aabb.min.fromLocal().toRemote().toAxisAlignedBB(aabb.max.fromLocal().toRemote())
        }
        val remoteCollisions = queryRemote(remotePortal.world, remoteAABB)

        // finally transform any collision boxes back to local space and add them to the result
        remoteCollisions.mapTo(aabbList) { aabb ->
            with(portal) { aabb.min.fromRemote().toLocal().toAxisAlignedBB(aabb.max.fromRemote().toLocal()) }
        }
    }

    /**
     * Called to determine if the given `queryAABB` (usually a shrunken entity BBs) is in the given material.
     * This differs from the vanilla method in that it takes into account blocks in the remote dimension, which vanilla
     * does not know about, and ignores those on the hidden side, which the entity should not interact with.
     *
     * This method is only called for selected portals (those which intersect with the entity BB).
     */
    fun isInMaterial(entity: Entity, queryAABB: AxisAlignedBB, material: Material): Boolean? {
        val world = entity.world

        val remotePortal = getRemoteAgent() ?: return null

        val portalPos = portal.localPosition.to3dMid()
        val entitySide = getEntitySide(entity)
        val hiddenSide = entitySide.opposite
        val entityHalf = AxisAlignedBB_INFINITE.with(entitySide.opposite, portalPos[entitySide.axis])
        val hiddenHalf = AxisAlignedBB_INFINITE.with(hiddenSide.opposite, portalPos[hiddenSide.axis])

        // For sanity, pretend there are no recursive portals

        // Split BB into local and remote side
        if (queryAABB.intersects(entityHalf)) {
            val localAABB = queryAABB.intersect(entityHalf)
            if (world.isMaterialInBB(localAABB, material)) {
                return true
            }
        }
        if (queryAABB.intersects(hiddenHalf)) {
            val aabb = queryAABB.intersect(hiddenHalf)
            val remoteAABB = with(portal) {
                aabb.min.fromLocal().toRemote().toAxisAlignedBB(aabb.max.fromLocal().toRemote())
            }
            if (remotePortal.world.isMaterialInBB(remoteAABB, material)) {
                return true
            }
        }
        return false
    }

    /**
     * Determines whether the given entity may be teleported by this agent.
     * Defaults to [Entity.isNonBoss].
     */
    protected open fun isEligibleForTeleportation(entity: Entity) = entity.isNonBoss

    /**
     * Checks if any entities have moved through the portal surface by comparing their positions to the previous call
     * and calls [teleport] for any which have.
     *
     * May teleport some entities and as such **must not** be called while ticking the world.
     */
    open fun checkTeleportees() {
        val facingVec = portal.localFacing.directionVec.to3d().abs() * 2
        val largerBB = portal.localBoundingBox.grow(facingVec)
        val finerBBs = portal.localDetailedBounds.map { it.grow(facingVec) }
        val seenEntities = mutableSetOf<Entity>()
        world.getEntitiesWithinAABB(Entity::class.java, largerBB).forEach {
            if (it is Portal) return@forEach
            if (it.isRiding) return@forEach
            if (!seenEntities.add(it)) return@forEach
            if (!isEligibleForTeleportation(it)) return@forEach

            val entityBB = it.entityBoundingBox
            if (finerBBs.any(entityBB::intersects)) {
                checkTeleportee(it)
            }
        }
        lastTickPos.keys.removeIf { !seenEntities.contains(it) }
    }

    /**
     * Updates the entity's position which is used for comparision in [checkTeleportee].
     *
     * This method must be called on the remote portal right after portal usage to prevent the entity from ending
     * up on incorrect sides or immediately teleporting back.
     */
    protected open fun updateEntityPosWithoutTeleport(entity: Entity) {
        lastTickPos[entity] = entity.pos + entity.eyeOffset
    }

    /**
     * Checks if the given entity has moved through the portal since the last time it has been called.
     * If it has, [teleport] is called with it.
     *
     * May teleport some entities and as such **must not** be called while ticking the world.
     * The given entity [isEligibleForTeleportation].
     */
    protected open fun checkTeleportee(entity: Entity) {
        val portalPos = portal.localPosition.to3dMid()
        val entityPos = entity.pos + entity.eyeOffset
        val entityPrevPos = lastTickPos[entity].also {
            lastTickPos[entity] = entityPos
        } ?: return
        val relPos = entityPos - portalPos
        val prevRelPos = entityPrevPos - portalPos
        val from = portal.localAxis.toFacing(relPos)
        val prevFrom = portal.localAxis.toFacing(prevRelPos)

        if (from != prevFrom) {
            teleport(entity, prevFrom)
        }
    }

    /**
     * Teleport the given entity (which has entered the portal from the given side) to the remote end.
     * For the player entity on the client, it calls [teleportPlayer].
     * For non-player entities on the server-side, it calls [teleportNonPlayerEntity].
     *
     * May teleport some entities and as such **must not** be called while ticking the world.
     *
     * Supports vehicles with passengers as long as the vehicle is not a player.
     * It is an error to call this for any passenger.
     */
    protected open fun teleport(entity: Entity, from: EnumFacing) {
        if (entity.isRiding) {
            throw IllegalArgumentException("Entity $entity is a passenger.")
        }

        if (entity is EntityPlayer) {
            if (entity.isBeingRidden) {
                return // not supported for now (too complicated for too little gain, not even possible in vanilla)
            }
            if (world.isRemote) teleportPlayer(entity, from)
        } else {
            if (!world.isRemote) teleportNonPlayerEntity(entity, from)
        }
    }

    //
    //  Server-side
    //

    /**
     * Teleport the given entity (which has entered the portal from the given side) to the remote end.
     *
     * May teleport some entities and as such **must not** be called while ticking the world.
     *
     * Supports non-player vehicles with passengers which may be players or other entities.
     */
    protected open fun teleportNonPlayerEntity(entity: Entity, from: EnumFacing) {
        val remotePortal = getRemoteAgent()!!
        val localWorld = world as WorldServer
        val remoteWorld = remotePortal.world as WorldServer

        if (!ForgeHooks.onTravelToDimension(entity, remotePortal.portal.localDimension)) return

        val remoteEntity = EntityList.newEntity(entity.javaClass, remoteWorld) ?: return
        val remoteEntities = mutableMapOf(entity to remoteEntity)
        val passengers = entity.recursivePassengers
        passengers.associateWithTo(remoteEntities) {
            it as? EntityPlayerMP ?: EntityList.newEntity(it.javaClass, remoteWorld) ?: return
        }

        // Inform other clients that the entity is going to be teleported
        // Don't bother informing those that don't track the bottom most entity, they can't react to it anyway
        val trackingPlayers = localWorld.entityTracker.getTracking(entity).filterTo(mutableSetOf()) {
            views.containsKey(it.viewManager)
        }
        trackingPlayers.forEach { it.viewManager.beginTransaction() }

        fun transfer(entity: Entity): Entity? {
            val newPassengers = entity.passengers.mapNotNull { transfer(it) }

            // Sync the entity's movement in the previous tick with clients (the regular entity tracker tick would have
            // happened only after this method is called, which is good since we want all in one transaction anyway).
            // Syncing is important because the client will re-position the new entity based on the position of the old
            // one, so all interpolation is preserved.
            // The entity will subsequently be removed from the local world, so no double-ticking is happening.
            localWorld.entityTracker.entries.find { it.trackedEntity == entity }?.updatePlayerList(remoteWorld.playerEntities)

            manager.serverBeforeUsePortal(this, entity, trackingPlayers)

            val newEntity = if (entity is EntityPlayerMP) {
                val viewManager = entity.viewManager
                val ticket = views[viewManager]
                if (ticket == null) {
                    manager.logger.warn("Player $entity is using portal $this as passenger but doesn't have a view for it!?")
                    return null
                }
                val view = ticket.view

                val remoteTicket = remotePortal.views[viewManager]
                if (remoteTicket == null) {
                    manager.logger.warn("Player $entity is using portal $this but our remote doesn't have a ticket for it!?")
                    return null
                }

                // Update view position
                view.player.derivePosRotFrom(entity, portal)

                // Forcefully dismount player without changing its position
                // We cannot just use dismountRidingEntity() as that'll send a position packet to the client.
                // There's no need to remove them from their vehicle because it'll be killed in the transfer anyway.
                entity.ridingEntity = null

                // Swap views
                trackingPlayers.remove(entity)
                trackingPlayers.add(view.player)
                view.makeMainView(ticket)

                entity
            } else {
                val newEntity = remoteEntities[entity] ?: return null

                localWorld.removeEntityDangerously(entity)
                localWorld.resetUpdateEntityTick()

                entity.dimension = remotePortal.portal.localDimension
                entity.isDead = false
                newEntity.readFromNBT(entity.writeToNBT(NBTTagCompound()))
                entity.isDead = true

                newEntity.derivePosRotFrom(entity, portal)

                remoteWorld.forceSpawnEntity(newEntity)

                // We need to tick the entity tracker entry of the new entity right now:
                // Above spawn call has sent out the entity's current position to players but the tracker won't store
                // the entity's current position until it's ticked. If we do not tick it now, the remote world may update
                // the new entity before ticking the tracker which will then store an updated position leading to incorrect
                // delta updates being sent to players and ultimately a desynced entity on the client.
                // AFAICT this is a vanilla bug. Though I'd imagine it's far more difficult to observe there.
                remoteWorld.entityTracker.entries.find { it.trackedEntity == newEntity }?.updatePlayerList(remoteWorld.playerEntities)

                // TODO Vanilla does an update here, not sure if that's necessary?
                //remoteWorld.updateEntityWithOptionalForce(newEntity, false)

                newEntity
            }

            if (newPassengers.isNotEmpty()) {
                newPassengers.forEach { it.startRiding(newEntity, true) }
                // Manually send passenger information to players
                // Otherwise they'll only be sent the next time the entity tracker is ticked (after the transaction)
                remoteWorld.entityTracker.entries.find { it.trackedEntity == newEntity }
                        ?.sendToTrackingAndSelf(SPacketSetPassengers(newEntity))
            }

            // Inform other clients that the teleportation has happened
            trackingPlayers.forEach { it.viewManager.flushPackets() }
            manager.serverAfterUsePortal(this, newEntity, trackingPlayers)

            return newEntity
        }
        transfer(entity)

        remoteWorld.resetUpdateEntityTick()

        // make sure the remote portal has the current position
        // otherwise, if the entity immediately reverses direction, it'll be on the wrong side by the next tick
        remotePortal.updateEntityPosWithoutTeleport(remoteEntity)

        trackingPlayers.forEach { it.viewManager.endTransaction() }
    }

    private val views = mutableMapOf<ServerViewManager, T>()
    private val tracking = mutableMapOf<ServerViewManager, Int>()

    open fun serverPortalUsed(player: EntityPlayerMP): Boolean {
        val viewManager = player.viewManager
        val ticket = views[viewManager]
        if (ticket == null) {
            manager.logger.warn("Received use portal request from $player which has no view for portal $this")
            return false
        }
        val view = ticket.view

        val remotePortal = getRemoteAgent()
        if (remotePortal == null) {
            manager.logger.warn("Received use portal request from $player for $this but our remote portal has vanished!?")
            return false
        }

        val remoteTicket = remotePortal.views[viewManager]
        if (remoteTicket == null) {
            manager.logger.warn("Received use portal request from $player for $this but our remote portal has no ticket!?")
            return false
        }

        // Update view position
        view.player.derivePosRotFrom(player, portal)

        // Inform other clients that the entity is going to be teleported
        val trackingPlayers = player.serverWorld.entityTracker.getTracking(player).filterTo(mutableSetOf()) {
            views.containsKey(it.viewManager)
        }
        trackingPlayers.forEach { it.viewManager.beginTransaction() }
        manager.serverBeforeUsePortal(this, player, trackingPlayers)

        // Swap views
        if (!view.isMainView) {
            if (view.player in trackingPlayers) {
                trackingPlayers.remove(view.player)
                trackingPlayers.add(player)
            }
            view.makeMainView(ticket)
        }

        // Inform other clients that the teleportation has happened
        trackingPlayers.forEach { it.viewManager.flushPackets() }
        manager.serverAfterUsePortal(this, player, trackingPlayers)
        trackingPlayers.forEach { it.viewManager.endTransaction() }

        // In case of horizontal portals, be nice and protect the player from fall damage for the next 10 seconds
        if (portal.plane == EnumFacing.Plane.HORIZONTAL && preventFallAfterVerticalPortal) {
            PreventNextFallDamage(player)
        }

        return true
    }

    open fun addTrackingPlayer(player: EntityPlayerMP) {
        val viewManager = player.viewManager

        tracking[viewManager] = (tracking[viewManager] ?: 0) + 1

        // If we already have a view for this player, then just link to it.
        // This can happen either because this is the remote portal to some other portal which we've already dealt with
        // or when multiple view entities have the same portal nearby.
        val ticket = views.getOrPut(viewManager) {
            // otherwise, it's time to find a suitable view
            val remotePortal = getRemoteAgent() ?: return
            val remoteWorld = remotePortal.world as WorldServer

            // Disable recursive portals, to be removed once cycle detection (and some recursion limit) is in place
            if (viewManager.player != player) return

            // Allocate a ticket for our local world which we can later give to our remote end
            // If this fails, someone is holding an exclusive ticket to our current world, so we fail soft
            val localView = player.view ?: return
            val localTicket = allocateTicket(localView) ?: return
            remotePortal.views[viewManager] = localTicket

            // preferably an existing view close by (half server view distance, ignoring y axis)
            val ticket = viewManager.views
                    .asSequence()
                    .filter { it.player.world == remoteWorld }
                    .map { it to it.player.pos.withoutY().distanceTo(portal.remotePosition.to3d().withoutY()) }
                    // at most half of server view distance between cam and portal
                    .filter { it.second < world.minecraftServer!!.playerList.entityViewDistance / 2 }
                    .sortedBy { it.second }
                    .fold(null as T?) { acc, view ->
                        acc ?: allocateTicket(view.first)
                    }
                    // but we'll also create a new one if we can't find one
                    ?: allocateTicket(viewManager.createView(remoteWorld, portal.remotePosition.to3d()))!!

            // If the remote portal is at first not linked due to the recursion-limit, then we need to manually link it
            // This can happen if you have two overworld portals with a bit of distance between with their two nether ends
            // close to each other. If you then only load one of the overworld portals, both nether portals will be loaded
            // but one won't yet be linked since that would require recursive loading (its overworld end is out of view).
            // If you then move to bring the other overworld portal into view, it (`this`) will link to the already existing
            // view in the nether but its nether end (`remotePortal`) won't be linked back on the client.
            if (viewManager in remotePortal.tracking) {
                manager.linkPortal(remotePortal, ticket.view.player, localTicket)
            }

            ticket
        }

        manager.linkPortal(this, player, ticket)
    }

    open fun removeTrackingPlayer(player: EntityPlayerMP) {
        val viewManager = player.viewManager

        manager.unlinkPortal(this, player)

        val remaining = tracking[viewManager]!! - 1
        if (remaining <= 0) {
            views.remove(viewManager)?.release()
        }
    }

    //
    // Client-side
    //

    @SideOnly(Side.CLIENT)
    var view: ClientView? = null

    @SideOnly(Side.CLIENT)
    private var portalUser: Entity? = null

    @SideOnly(Side.CLIENT)
    fun beforeUsePortal(entity: Entity) {
        portalUser = entity

        // If this is the client player, then a swap of main view (and view entities!) is soon to come.
        // As such, by the time [afterUsePortal] is called, the portalUser in this world will be the view entity,
        // not the player entity.
        if (entity is EntityPlayerSP) {
            portalUser = view?.player
            if (portalUser == null) {
                manager.logger.warn("Got pre portal usage message for client player on $this even though no view is set")
            }
        }
    }

    @SideOnly(Side.CLIENT)
    fun afterUsePortal(entityId: Int) {
        val entity = portalUser
        portalUser = null
        if (entity == null) {
            manager.logger.warn("Got unexpected post portal usage message for $this by entity with new id $entityId")
            return
        }
        if (!entity.isDead && entity !is EntityPlayerSP) {
            manager.logger.warn("Entity $entity is still alive post portal usage!")
        }

        val view = view
        if (view == null) {
            manager.logger.warn("Failed syncing of $entity after usage of portal $this because view has not been set")
            return
        }

        val newEntity = view.world.getEntityByID(entityId)
        if (newEntity == null) {
            manager.logger.warn("Oh no! The entity $entity with new id $entityId did not reappear at the other side of $this!")
            return
        }

        val pos = newEntity.pos
        val yaw = newEntity.rotationYaw
        val pitch = newEntity.rotationPitch
        newEntity.derivePosRotFrom(entity, portal)
        if (newEntity is EntityOtherPlayerMP) {
            newEntity.otherPlayerMPPos = pos // preserve otherPlayerMP pos to prevent desync
            newEntity.otherPlayerMPYaw = yaw.toDouble()
            newEntity.otherPlayerMPPitch = pitch.toDouble()
            newEntity.otherPlayerMPPosRotationIncrements = 3 // and sudden jumps
        }
        if (newEntity is EntityMinecart) {
            newEntity.minecartPos = pos // preserve minecart pos to prevent desync
            newEntity.minecartYaw = yaw.toDouble()
            newEntity.minecartPitch = pitch.toDouble()
            newEntity.turnProgress = 3 // and sudden jumps
        }
        if (newEntity is AbstractClientPlayer && entity is AbstractClientPlayer) {
            newEntity.ticksElytraFlying = entity.ticksElytraFlying
            newEntity.rotateElytraX = entity.rotateElytraX
            newEntity.rotateElytraY = entity.rotateElytraY
            newEntity.rotateElytraZ = entity.rotateElytraZ
        }
    }

    @SideOnly(Side.CLIENT)
    protected open fun teleportPlayer(player: EntityPlayer, from: EnumFacing): Boolean {
        if (player !is EntityPlayerSP || player.entityId < 0) return false

        val view = view
        if (view == null) {
            manager.logger.warn("Failed to use portal $this because view has not been set")
            return false
        }
        view.clientPlayer.deriveClientPosRotFrom(player, portal)

        val remotePortal = getRemoteAgent()
        if (remotePortal == null) {
            manager.logger.warn("Failed to use portal $this because remote portal in $view couldn't be found")
            return false
        }

        view.makeMainView()
        manager.clientUsePortal(this)

        // make sure the remote portal has the current position
        // otherwise, if the entity immediately reverses direction, it'll be on the wrong side by the next tick
        remotePortal.updateEntityPosWithoutTeleport(player)
        // also update the position for this portal in case we stayed within the same view
        updateEntityPosWithoutTeleport(player)

        return true
    }

    @SideOnly(Side.CLIENT)
    open fun canBeSeen(camera: ICamera): Boolean =
            camera.isBoundingBoxInFrustum(portal.localBoundingBox)
                    && portal.localDetailedBounds.any { camera.isBoundingBoxInFrustum(it) }
}