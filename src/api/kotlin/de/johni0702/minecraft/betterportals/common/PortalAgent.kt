package de.johni0702.minecraft.betterportals.common

import de.johni0702.minecraft.betterportals.client.deriveClientPosRotFrom
import de.johni0702.minecraft.view.client.worldsManager
import de.johni0702.minecraft.view.server.*
import net.minecraft.block.material.Material
import net.minecraft.client.Minecraft
import net.minecraft.client.entity.AbstractClientPlayer
import net.minecraft.client.entity.EntityOtherPlayerMP
import net.minecraft.client.entity.EntityPlayerSP
import net.minecraft.client.multiplayer.WorldClient
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

interface PortalAccessor {
    /**
     * Collection of all, currently loaded portals known to this accessor.
     *
     * When implementing, take note of [onChange].
     */
    val loadedPortals: Iterable<PortalAgent<*>>

    /**
     * Retrieve an agent by id. See [PortalAgent.id].
     */
    fun findById(id: ResourceLocation): PortalAgent<*>?

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
    val loadedPortals: Iterable<PortalAgent<*>>

    /**
     * Registers a new source for [loadedPortals].
     */
    fun registerPortals(accessor: PortalAccessor)

    /**
     * Retrieve an agent by id. See [PortalAccessor.findById].
     */
    fun findById(id: ResourceLocation): PortalAgent<*>?

    /**
     * Called immediately after the client player uses a portal to inform the server of that action.
     * Calls [PortalAgent.serverPortalUsed] on the corresponding agent on the server.
     *
     * Passing an agent which is not in [loadedPortals] is an error.
     */
    @SideOnly(Side.CLIENT)
    fun clientUsePortal(agent: PortalAgent<*>)

    /**
     * Called right before an entity uses a portal to ensure clients are prepared for it.
     * Calls [PortalAgent.beforeUsePortal] on the corresponding agent on the client.
     *
     * Passing an agent which is not in [loadedPortals] is an error.
     */
    fun serverBeforeUsePortal(agent: PortalAgent<*>, oldEntity: Entity, trackingPlayers: Iterable<ServerWorldsManager>)

    /**
     * Called right after an entity uses a portal to allow clients to properly display the transition.
     * Calls [PortalAgent.afterUsePortal] on the corresponding agent on the client.
     *
     * Passing an agent which is not in [loadedPortals] is an error.
     */
    fun serverAfterUsePortal(agent: PortalAgent<*>, newEntity: Entity, trackingPlayers: Iterable<ServerWorldsManager>)

    /**
     * Whether to (by default) prevent the next fall damage after passing through a vertical portal.
     */
    val preventFallAfterVerticalPortal: Boolean
}

val World.portalManager get() = BetterPortalsAPI.instance.getPortalManager(this)

open class PortalAgent<P: Portal>(
        val manager: PortalManager,
        /**
         * A unique (per world/manager) id for this agent.
         * The id must be shared with the corresponding agent on the client/server side and at least one portal accessor
         * registered must be able to resolve it via [PortalAccessor.findById] on either side.
         */
        open val id: ResourceLocation,
        portal: P,
        val portalConfig: PortalConfiguration
) {
    var portal = portal
        set(value) {
            // If we moved, invalidate all tracked entity positions
            if (value.localAxis != field.localAxis || value.localPosition != field.localPosition) {
                lastTickPos.clear()
            }

            field = value

            if (!world.isRemote) {
                // Always refresh views since either anchor or target have almost certainly changed
                updateViews()
            }
        }
    val world get() = manager.world
    open val preventFallAfterVerticalPortal get() = manager.preventFallAfterVerticalPortal

    open fun isLinked(other: PortalAgent<*>): Boolean =
            other.portal.isTarget(portal) && portal.isTarget(other.portal)

    val remoteWorld: World?
        get() = if (world.isRemote) {
            remoteClientWorld
        } else {
            portal.remoteDimension?.let { world.minecraftServer!!.getWorld(it) }
        }

    val remoteAgent: PortalAgent<P>?
        @Suppress("UNCHECKED_CAST")
        get() = remoteWorld?.portalManager?.loadedPortals?.find { isLinked(it) } as PortalAgent<P>?

    /**
     * Loads and returns the remote agent.
     * Note that this may cause chunks to load (which in turn loads entitys and potentiall more portals) which can be
     * unsafe in certain contexts, e.g. while iterating over [net.minecraft.entity.EntityTracker.entries] (as may be
     * the case when [Entity.addTrackingPlayer] is called) or [PortalManager.loadedPortals].
     */
    fun loadRemoteAgent(): PortalAgent<P>? {
        remoteWorld?.getBlockState(portal.remotePosition) // make sure the portal is loaded
        return remoteAgent
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
        val remotePortal = remoteAgent
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
    open fun isInMaterial(entity: Entity, queryAABB: AxisAlignedBB, material: Material): Boolean? {
        val world = entity.world

        val remotePortal = remoteAgent ?: return null

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
        remoteAgent ?: return // not linked or remote not loaded
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
        val remotePortal = remoteAgent!!
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
        val trackingPlayers = localWorld.entityTracker.getTracking(entity).map { it.worldsManager }
        trackingPlayers.forEach { it.beginTransaction() }

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
                if (localWorld == remoteWorld) {
                    entity.derivePosRotFrom(entity, portal)
                    entity.connection.captureCurrentPosition()
                } else {
                    val worldsManager = entity.worldsManager

                    // Forcefully dismount player without changing its position
                    // We cannot just use dismountRidingEntity() as that'll send a position packet to the client.
                    // There's no need to remove them from their vehicle because it'll be killed in the transfer anyway.
                    entity.ridingEntity = null

                    worldsManager.changeDimension(remoteWorld) {
                        derivePosRotFrom(this, portal)
                    }
                }

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
            trackingPlayers.forEach { it.flushPackets() }
            manager.serverAfterUsePortal(this, newEntity, trackingPlayers)

            return newEntity
        }
        transfer(entity)

        remoteWorld.resetUpdateEntityTick()

        // make sure the remote portal has the current position
        // otherwise, if the entity immediately reverses direction, it'll be on the wrong side by the next tick
        remotePortal.updateEntityPosWithoutTeleport(remoteEntity)

        trackingPlayers.forEach { it.endTransaction() }
    }

    private val views = mutableMapOf<EntityPlayerMP, View?>()

    open fun serverPortalUsed(player: EntityPlayerMP): Boolean {
        val worldsManager = player.worldsManager

        val remotePortal = remoteAgent
        if (remotePortal == null) {
            manager.logger.warn("Received use portal request from $player for $this but our remote portal has vanished!?")
            return false
        }

        // Inform other clients that the entity is going to be teleported
        val trackingPlayers = player.serverWorld.entityTracker.getTracking(player).map { it.worldsManager }
        trackingPlayers.forEach { it.beginTransaction() }
        manager.serverBeforeUsePortal(this, player, trackingPlayers)

        // Teleport
        if (world == remotePortal.world) {
            player.derivePosRotFrom(player, portal)
            player.connection.captureCurrentPosition()
        } else {
            worldsManager.changeDimension(remotePortal.world as WorldServer) {
                derivePosRotFrom(this, portal)
            }
        }

        // Inform other clients that the teleportation has happened
        trackingPlayers.forEach { it.flushPackets() }
        manager.serverAfterUsePortal(this, player, trackingPlayers)
        trackingPlayers.forEach { it.endTransaction() }

        // In case of horizontal portals, be nice and protect the player from fall damage for the next 10 seconds
        if (portal.plane == EnumFacing.Plane.HORIZONTAL && preventFallAfterVerticalPortal) {
            PreventNextFallDamage(player)
        }

        return true
    }

    open fun addTrackingPlayer(player: EntityPlayerMP) {
        views.getOrPut(player) {
            val remoteWorld = remoteWorld as WorldServer? ?: return@getOrPut null
            val anchor = Pair(world as WorldServer, portal.localPosition.toCubePos())
            // TODO use view with reduced load distance to put a sensible limit on recursion
            player.worldsManager.createView(remoteWorld, portal.remotePosition.to3dMid(), anchor)
        }
    }

    open fun removeTrackingPlayer(player: EntityPlayerMP) {
        views.remove(player)?.dispose()
    }

    open fun updateViews() {
        if (views.isEmpty()) {
            return
        }

        val oldViews = views.toMap()
        views.clear()
        oldViews.keys.forEach(this::addTrackingPlayer)
        oldViews.values.forEach { it?.dispose() }
    }

    //
    // Client-side
    //

    val remoteClientWorld: WorldClient?
        @SideOnly(Side.CLIENT)
        get() = Minecraft.getMinecraft().worldsManager?.worlds?.find { it.provider.dimension == portal.remoteDimension }

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
        if (!entity.isDead && entity !is EntityPlayerSP) {
            manager.logger.warn("Entity $entity is still alive post portal usage!")
        }

        val remoteWorld = remoteWorld
        if (remoteWorld == null) {
            manager.logger.warn("Failed syncing of $entity after usage of portal $this because remote world is not loaded")
            return
        }

        val newEntity = remoteWorld.getEntityByID(entityId)
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

        val remoteWorld = remoteClientWorld
        if (remoteWorld == null) {
            manager.logger.warn("Failed to use portal $this because remote world is not loaded")
            return false
        }

        val remotePortal = remoteAgent
        if (remotePortal == null) {
            manager.logger.warn("Failed to use portal $this because remote portal in $remoteWorld couldn't be found")
            return false
        }

        if (remoteWorld == world) {
            player.deriveClientPosRotFrom(player, portal)
        } else {
            player.worldsManager.changeDimension(remoteWorld) {
                deriveClientPosRotFrom(this, portal)
            }
        }
        manager.clientUsePortal(this)

        // make sure the remote portal has the current position
        // otherwise, if the entity immediately reverses direction, it'll be on the wrong side by the next tick
        remotePortal.updateEntityPosWithoutTeleport(player)
        // also update the position for this portal in case we stayed within the same world
        updateEntityPosWithoutTeleport(player)

        return true
    }

    @SideOnly(Side.CLIENT)
    open fun canBeSeen(camera: ICamera): Boolean =
            camera.isBoundingBoxInFrustum(portal.localBoundingBox)
                    && portal.localDetailedBounds.any { camera.isBoundingBoxInFrustum(it) }

    /**
     * Given the side of the portal from which it is being rendered, returns the offset of the clipping plane in that
     * direction.
     * The clipping plane is used to hide geometry in the remote world which would occlude the portal even though it
     * should be invisible. E.g. blocks and entities between the remote camera and the remote portal.
     *
     * This should be as large as feasible to reduce the impact of https://github.com/Johni0702/BetterPortals/issues/75.
     * Usually this is 0.5, i.e. right at the end of the portal frame such that the whole remote portal frame
     * is rendered and then drawn on the local side.
     *
     * If the portal block itself has a back plane which resides in the same block (e.g. TravelHuts portals), then the
     * value needs to be reduced as otherwise the remote back plane would be visible, occluding everything else.
     */
    open fun getClippingPlaneOffset(cameraSide: EnumFacing) = 0.5
}