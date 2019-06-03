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
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.nbt.NBTTagCompound
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

interface PortalAccessor<T: CanMakeMainView> {
    /**
     * Collection of all, currently loaded portals known to this accessor.
     */
    val loadedPortals: Iterable<PortalAgent<T, *>>

    /**
     * Retrieve an agent by id. See [PortalAgent.id].
     */
    fun findById(id: ResourceLocation): PortalAgent<T, *>?
}

interface PortalManager {
    val logger: Logger
    val world: World

    /**
     * Collection of all, currently loaded portals.
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
     * Called right before a non-player entity uses a portal to ensure clients are prepared for it.
     * Calls [PortalAgent.beforeUsePortal] on the corresponding agent on the client.
     *
     * Passing an agent which is not in [loadedPortals] is an error.
     */
    fun serverBeforeUsePortal(agent: PortalAgent<*, *>, oldEntity: Entity, trackingPlayers: Iterable<EntityPlayerMP>)

    /**
     * Called right after a non-player entity uses a portal to allow clients to properly display the transition.
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
        val allocateTicket: (ServerView) -> T?
) {
    val world get() = manager.world
    open val preventFallAfterVerticalPortal get() = manager.preventFallAfterVerticalPortal

    open fun isLinked(other: PortalAgent<*, *>): Boolean =
            other.portal.isTarget(portal) && portal.isTarget(other.portal)

    open fun getRemoteAgent(): PortalAgent<T, P>? {
        val remoteWorld = if (world.isRemote) {
            (view ?: return null).camera.world
        } else {
            world.minecraftServer!!.getWorld(portal.remoteDimension ?: return null)
        }
        remoteWorld.getChunkFromBlockCoords(portal.remotePosition) // make sure the portal is loaded
        val remotePortal = remoteWorld.portalManager.loadedPortals.find { isLinked(it) }
        @Suppress("UNCHECKED_CAST")
        return remotePortal as PortalAgent<T, P>?
    }

    private var lastTickPos = mutableMapOf<Entity, Vec3d>()
    private var thisTickPos = mutableMapOf<Entity, Vec3d>()

    /**
     * Returns the side of the portal on which the entity resides.
     * This method takes into account that even though the entity might have already passed through the portal surface
     * it might have not yet been teleported. In such cases, this method returns the logical side (i.e. the one on
     * which the entity entered the portal), not the actual side one would get by naively comparing coordinates.
     */
    fun getEntitySide(entity: Entity): EnumFacing {
        val entityPos = lastTickPos[entity] ?: (entity.pos + entity.eyeOffset)
        return portal.localFacing.axis.toFacing(entityPos - portal.localPosition.to3d())
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
     * Checks if any entities have moved through the portal surface by comparing their positions to the previous call
     * and calls [teleport] for any which have.
     *
     * May teleport some entities and as such **must not** be called while ticking the world.
     */
    open fun checkTeleportees() {
        val facingVec = portal.localFacing.directionVec.to3d().abs() * 2
        val largerBB = portal.localBoundingBox.grow(facingVec)
        val finerBBs = portal.localDetailedBounds.map { it.grow(facingVec) }
        world.getEntitiesWithinAABB(Entity::class.java, largerBB).forEach {
            if (it is Portal) return@forEach
            val entityBB = it.entityBoundingBox
            if (finerBBs.any(entityBB::intersects)) {
                checkTeleportee(it)
            }
        }
        lastTickPos = thisTickPos.also {
            thisTickPos = lastTickPos
            thisTickPos.clear()
        }
    }

    /**
     * Checks if the given entity has moved through the portal since the last time [checkTeleportees] has been called.
     * If it has, [teleport] is called with it.
     *
     * There's little point in calling this outside of [checkTeleportees].
     *
     * May teleport some entities and as such **must not** be called while ticking the world.
     */
    protected open fun checkTeleportee(entity: Entity) {
        val portalPos = portal.localPosition.to3dMid()
        val entityPos = entity.pos + entity.eyeOffset
        thisTickPos[entity] = entityPos
        val entityPrevPos = lastTickPos[entity] ?: return
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
     */
    protected open fun teleport(entity: Entity, from: EnumFacing) {
        thisTickPos.remove(entity)

        if (entity.isRiding || entity.isBeingRidden) {
            return // just do nothing for now, not even dismounting works as one would hope
        }

        if (entity is EntityPlayer) {
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
     */
    protected open fun teleportNonPlayerEntity(entity: Entity, from: EnumFacing) {
        val remotePortal = getRemoteAgent()!!
        val localWorld = world as WorldServer
        val remoteWorld = remotePortal.world as WorldServer

        if (!ForgeHooks.onTravelToDimension(entity, remotePortal.portal.localDimension)) return

        val newEntity = EntityList.newEntity(entity.javaClass, remoteWorld) ?: return

        // Inform other clients that the entity is going to be teleported
        val trackingPlayers = localWorld.entityTracker.getTracking(entity).filterTo(mutableSetOf()) {
            views.containsKey(it.viewManager)
        }
        trackingPlayers.forEach { it.viewManager.beginTransaction() }
        manager.serverBeforeUsePortal(this, entity, trackingPlayers)

        localWorld.removeEntityDangerously(entity)
        localWorld.resetUpdateEntityTick()

        entity.dimension = remotePortal.portal.localDimension
        entity.isDead = false
        newEntity.readFromNBT(entity.writeToNBT(NBTTagCompound()))
        entity.isDead = true

        newEntity.derivePosRotFrom(entity, portal)

        remoteWorld.forceSpawnEntity(newEntity)
        // TODO Vanilla does an update here, not sure if that's necessary?
        //remoteWorld.updateEntityWithOptionalForce(newEntity, false)
        remoteWorld.resetUpdateEntityTick()

        // Inform other clients that the teleportation has happened
        trackingPlayers.forEach { it.viewManager.flushPackets() }
        manager.serverAfterUsePortal(this, newEntity, trackingPlayers)
        trackingPlayers.forEach { it.viewManager.endTransaction() }
    }

    private val views = mutableMapOf<ServerViewManager, T>()

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
        view.camera.derivePosRotFrom(player, portal)

        // Inform other clients that the entity is going to be teleported
        val trackingPlayers = player.serverWorld.entityTracker.getTracking(player).filterTo(mutableSetOf()) {
            views.containsKey(it.viewManager)
        }
        trackingPlayers.forEach { it.viewManager.beginTransaction() }
        manager.serverBeforeUsePortal(this, player, trackingPlayers)

        // Swap views
        view.makeMainView(ticket)

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
            remotePortal.views[viewManager] = allocateTicket(localView) ?: return

            // preferably an existing view close by (64 blocks, ignoring y axis)
            viewManager.views
                    .asSequence()
                    .filter { it.camera.world == remoteWorld }
                    .map { it to it.camera.pos.withoutY().distanceTo(portal.remotePosition.to3d().withoutY()) }
                    .filter { it.second < 64 } // Arbitrarily chosen limit for max distance between cam and portal
                    .sortedBy { it.second }
                    .fold(null as T?) { acc, view ->
                        acc ?: allocateTicket(view.first)
                    }
                    // but we'll also create a new one if we can't find one
                    ?: allocateTicket(viewManager.createView(remoteWorld, portal.remotePosition.to3d()))!!
        }

        manager.linkPortal(this, player, ticket)
    }

    open fun removeTrackingPlayer(player: EntityPlayerMP) {
        views.remove(player.viewManager)?.let {
            manager.unlinkPortal(this, player)
            it.release()
        }
    }

    //
    // Client-side
    //

    @SideOnly(Side.CLIENT)
    var view: ClientView? = null

    @SideOnly(Side.CLIENT)
    protected open fun onClientUpdate() {
        val player = world.getPlayers(EntityPlayerSP::class.java) { true }[0]
        view?.let { view ->
            if (!view.isMainView) {
                view.camera.deriveClientPosRotFrom(player, portal)
            }
        }
    }

    @SideOnly(Side.CLIENT)
    private var portalUser: Entity? = null

    @SideOnly(Side.CLIENT)
    fun beforeUsePortal(entity: Entity) {
        portalUser = entity
    }

    @SideOnly(Side.CLIENT)
    fun afterUsePortal(entityId: Int) {
        val entity = portalUser
        portalUser = null
        if (entity == null) {
            manager.logger.warn("Got unexpected post portal usage message for $this by entity with new id $entityId")
            return
        }
        if (!entity.isDead) {
            manager.logger.warn("Entity $entity is still alive post portal usage!")
        }

        val view = view
        if (view == null) {
            manager.logger.warn("Failed syncing of $entity after usage of portal $this because view has not been set")
            return
        }

        val newEntity = view.camera.world.getEntityByID(entityId)
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
        view.camera.deriveClientPosRotFrom(player, portal)

        val remotePortal = getRemoteAgent()
        if (remotePortal == null) {
            manager.logger.warn("Failed to use portal $this because remote portal in $view couldn't be found")
            return false
        }

        view.makeMainView()
        manager.clientUsePortal(this)

        remotePortal.onClientUpdate()
        return true
    }

    @SideOnly(Side.CLIENT)
    open fun canBeSeen(camera: ICamera): Boolean =
            camera.isBoundingBoxInFrustum(portal.localBoundingBox)
                    && portal.localDetailedBounds.any { camera.isBoundingBoxInFrustum(it) }
}