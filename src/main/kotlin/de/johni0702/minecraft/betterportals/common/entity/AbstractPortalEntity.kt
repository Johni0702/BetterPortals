package de.johni0702.minecraft.betterportals.common.entity

import de.johni0702.minecraft.betterportals.LOGGER
import de.johni0702.minecraft.betterportals.client.UtilsClient
import de.johni0702.minecraft.betterportals.client.view.ClientView
import de.johni0702.minecraft.betterportals.common.*
import de.johni0702.minecraft.betterportals.net.*
import de.johni0702.minecraft.betterportals.server.view.ServerView
import de.johni0702.minecraft.betterportals.server.view.viewManager
import io.netty.buffer.ByteBuf
import net.minecraft.client.Minecraft
import net.minecraft.client.entity.AbstractClientPlayer
import net.minecraft.client.entity.EntityOtherPlayerMP
import net.minecraft.client.entity.EntityPlayerSP
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityList
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.network.PacketBuffer
import net.minecraft.util.EnumFacing
import net.minecraft.util.Rotation
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import net.minecraft.world.WorldServer
import net.minecraftforge.common.ForgeHooks
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.entity.living.LivingFallEvent
import net.minecraftforge.event.world.GetCollisionBoxesEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly

abstract class AbstractPortalEntity(
        world: World,
        override var plane: EnumFacing.Plane,
        override var relativeBlocks: Set<BlockPos>,
        override var localDimension: Int,
        localPosition: BlockPos,
        localRotation: Rotation,
        override var remoteDimension: Int?,
        override var remotePosition: BlockPos,
        override var remoteRotation: Rotation
) : Entity(world), Portal.Mutable, IEntityAdditionalSpawnData {

    override fun getRenderBoundingBox(): AxisAlignedBB = localBoundingBox
    override var localPosition = localPosition
        set(value) {
            field = value
            with(value.to3d()) { setPosition(x + 0.5, y + 0.5, z + 0.5) }
        }
    override var localRotation = localRotation
        set(value) {
            field = value
            setRotation(value.degrees.toFloat(), 0f)
        }

    init {
        // MC checks whether entities are completely inside the view frustum which is completely useless/broken for
        // large entities.
        // The sane solution would have been to check if they are partially inside it.
        // If required for performance, that check may be implemented in Render#shouldRender but I'm not doing it now.
        // I blame MC
        ignoreFrustumCheck = true

        width = 0f
        height = 0f

        with(localPosition.to3d()) { setPosition(x + 0.5, y + 0.5, z + 0.5) }
        this.setRotation(localRotation.degrees.toFloat(), 0f)
    }


    override fun entityInit() {
    }

    override fun onUpdate() {
        if (!EventHandler.registered) {
            EventHandler.registered = true
        }

        if (world.isRemote) {
            onClientUpdate()
        }
    }

    protected open fun checkTeleportees() {
        val facingVec = localFacing.directionVec.to3d().abs() * 2
        val largerBB = localBoundingBox.grow(facingVec)
        val finerBBs = localBlocks.map { AxisAlignedBB(it).grow(facingVec) }
        world.getEntitiesWithinAABBExcludingEntity(this, largerBB).forEach {
            val entityBB = it.entityBoundingBox
            if (finerBBs.any { entityBB.intersects(it) }) {
                checkTeleportee(it)
            }
        }
    }

    protected open fun checkTeleportee(entity: Entity) {
        val portalPos = pos
        val entityPos = entity.pos
        val entityPrevPos = entity.lastTickPos
        val relPos = entityPos - portalPos
        val prevRelPos = entityPrevPos - portalPos
        val from = localAxis.toFacing(relPos)
        val prevFrom = localAxis.toFacing(prevRelPos)

        if (from != prevFrom) {
            teleportEntity(entity, prevFrom)
        }
    }

    protected open fun teleportEntity(entity: Entity, from: EnumFacing) {
        if (entity is EntityPlayer) {
            if (world.isRemote) teleportPlayer(entity, from)
            return
        }

        if (!world.isRemote) {
            val remotePortal = getRemotePortal()!!
            val localWorld = world as WorldServer
            val remoteWorld = remotePortal.world as WorldServer

            if (!ForgeHooks.onTravelToDimension(entity, remotePortal.dimension)) return

            val newEntity = EntityList.newEntity(entity.javaClass, remoteWorld) ?: return

            // Inform other clients that the entity is going to be teleported
            val trackingPlayers = localWorld.entityTracker.getTracking(entity).intersect(views.keys)
            trackingPlayers.forEach {
                Transaction.start(it)
                it.viewManager.flushPackets()
            }
            EntityUsePortal(EntityUsePortal.Phase.BEFORE, entity.entityId, this.entityId).sendTo(trackingPlayers)

            localWorld.removeEntityDangerously(entity)
            localWorld.resetUpdateEntityTick()

            entity.dimension = remotePortal.dimension
            entity.isDead = false
            newEntity.readFromNBT(entity.writeToNBT(NBTTagCompound()))
            entity.isDead = true

            Utils.transformPosition(entity, newEntity, this)

            remoteWorld.forceSpawnEntity(newEntity)
            // TODO Vanilla does an update here, not sure if that's necessary?
            //remoteWorld.updateEntityWithOptionalForce(newEntity, false)
            remoteWorld.resetUpdateEntityTick()

            // Inform other clients that the teleportation has happened
            trackingPlayers.forEach { it.viewManager.flushPackets() }
            EntityUsePortal(EntityUsePortal.Phase.AFTER, newEntity.entityId, this.entityId).sendTo(trackingPlayers)
            trackingPlayers.forEach { Transaction.end(it) }
        }
    }

    override fun canBeAttackedWithItem(): Boolean = true
    override fun canBeCollidedWith(): Boolean = true
    override fun canBePushed(): Boolean = false
    override fun hitByEntity(entityIn: Entity?): Boolean = true

    override fun shouldSetPosAfterLoading(): Boolean = false
    override fun readEntityFromNBT(compound: NBTTagCompound) {
        readPortalFromNBT(compound.getTag("BetterPortal"))
    }

    override fun writeEntityToNBT(compound: NBTTagCompound) {
        compound.setTag("BetterPortal", writePortalToNBT())
    }

    override fun readSpawnData(additionalData: ByteBuf) {
        readPortalFromNBT(PacketBuffer(additionalData).readCompoundTag())
    }

    override fun writeSpawnData(buffer: ByteBuf) {
        PacketBuffer(buffer).writeCompoundTag(writePortalToNBT())
    }

    private object EventHandler {
        var registered by MinecraftForge.EVENT_BUS

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
            world.getEntities(AbstractPortalEntity::class.java, { it?.isDead == false }).forEach {
                it.checkTeleportees()
            }
        }

        @SubscribeEvent
        fun onGetCollisionBoxes(event: GetCollisionBoxesEvent) {
            event.world.getEntities(AbstractPortalEntity::class.java, { it?.isDead == false }).forEach {
                if (it.getRemotePortal() != null) return@forEach
                if (it.localBoundingBox.intersects(event.aabb)) {
                    it.localBlocks.forEach {
                        val blockAABB = AxisAlignedBB(it)
                        if (blockAABB.intersects(event.aabb)) {
                            event.collisionBoxesList.add(blockAABB)
                        }
                    }
                }
            }
        }
    }

    protected fun getRemotePortal(): AbstractPortalEntity? {
        val remoteWorld = if (world.isRemote) {
            (view ?: return null).camera.world
        } else {
            world.minecraftServer!!.getWorld(remoteDimension ?: return null)
        }
        return remoteWorld.getEntitiesWithinAABB(javaClass, AxisAlignedBB(remotePosition)).firstOrNull()
    }

    //
    //  Server-side
    //

    private val views = mutableMapOf<EntityPlayerMP, ServerView?>()

    internal open fun usePortal(player: EntityPlayerMP): Boolean {
        val view = views[player]
        if (view == null) {
            LOGGER.warn("Received use portal request from $player which has no view for portal $this")
            return false
        }

        // Forcefully set the views which will be assigned to the camera and player after the switch to make sure
        // they are the same as the one the client uses
        val remotePortal = getRemotePortal()!!
        with(this)         { views[view.camera] = views.remove(player)      }
        with(remotePortal) { views[player]      = views.remove(view.camera) }

        // Update view position
        Utils.transformPosition(player, view.camera, this)

        // Inform other clients that the entity is going to be teleported
        val trackingPlayers = player.serverWorld.entityTracker.getTracking(player).intersect(views.keys)
        trackingPlayers.forEach {
            Transaction.start(it)
            it.viewManager.flushPackets()
        }
        EntityUsePortal(EntityUsePortal.Phase.BEFORE, player.entityId, this.entityId).sendTo(trackingPlayers)

        // Swap views
        view.makeMainView()

        // Inform other clients that the teleportation has happened
        trackingPlayers.forEach { it.viewManager.flushPackets() }
        EntityUsePortal(EntityUsePortal.Phase.AFTER, player.entityId, this.entityId).sendTo(trackingPlayers)
        trackingPlayers.forEach { it.viewManager.flushPackets() }
        trackingPlayers.forEach { Transaction.end(it) }

        // In case of horizontal portals, be nice and protect the player from fall damage for the next 10 seconds
        if (plane == EnumFacing.Plane.HORIZONTAL) {
            PreventNextFallDamage(player)
        }

        return true
    }

    private val trackingPlayers = mutableListOf<EntityPlayerMP>()

    override fun addTrackingPlayer(player: EntityPlayerMP) {
        super.addTrackingPlayer(player)

        trackingPlayers.add(player)

        val viewManager = player.viewManager
        val viewId = if (viewManager.player != player) {
            // FIXME main view isn't always the right choice once portals are allowed to be recursive
            (views[player] ?: viewManager.mainView.also { it.retain() }.also { views[player] = it }).id
        } else {
            val remoteWorld = player.mcServer.getWorld(remoteDimension ?: return)
            // Choose already existing view
            val view = views[player] ?: (
                    // Or existing view close by (64 blocks, ignoring y axis)
                    viewManager.views
                            .filter { it.camera.world == remoteWorld }
                            .map { it to it.camera.pos.withoutY().distanceTo(remotePosition.to3d().withoutY()) }
                            .filter { it.second < 64 } // Arbitrarily chosen limit for max distance between cam and portal
                            .sortedBy { it.second }
                            .firstOrNull()
                            ?.first
                            // Or create a new one
                            ?: viewManager.createView(remoteWorld, remotePosition.to3d())
                    ).also { it.retain() }.also { views[player] = it }
            view.id
        }

        LinkPortal(
                entityId,
                writePortalToNBT(),
                viewId
        ).sendTo(player)
    }

    override fun removeTrackingPlayer(player: EntityPlayerMP) {
        super.removeTrackingPlayer(player)

        trackingPlayers.remove(player)

        views.remove(player)?.release()
    }

    override fun link(remoteDimension: Int, remotePosition: BlockPos, remoteRotation: Rotation) {
        if (this.remoteDimension != null) {
            // Unlink all tracking players
            trackingPlayers.toList().forEach {
                removeTrackingPlayer(it)
            }
        }

        super.link(remoteDimension, remotePosition, remoteRotation)

        // Update tracking players
        trackingPlayers.toList().forEach {
            addTrackingPlayer(it)
        }
    }

    override fun setDead() {
        if (isDead) return
        super.setDead()
        if (world is WorldServer) {
            getRemotePortal()?.setDead()
            removePortal()
        }
    }

    protected open fun removePortal() {
        localBlocks.forEach { world.setBlockToAir(it) }
    }

    //
    // Client-side
    //

    @SideOnly(Side.CLIENT)
    var view: ClientView? = null

    @SideOnly(Side.CLIENT)
    override fun isInRangeToRenderDist(distance: Double): Boolean = true // MC makes this depend on entityBoundingBox

    @SideOnly(Side.CLIENT)
    protected open fun onClientUpdate() {
        val player = world.getPlayers(EntityPlayerSP::class.java) { true }[0]
        view?.let { view ->
            if (!view.isMainView) {
                UtilsClient.transformPosition(player, view.camera, this)
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
            LOGGER.warn("Got unexpected post portal usage message for $this by entity with new id $entityId")
            return
        }
        if (!entity.isDead) {
            LOGGER.warn("Entity $entity is still alive post portal usage!")
        }

        val view = view
        if (view == null) {
            LOGGER.warn("Failed syncing of $entity after usage of portal $this because view has not been set")
            return
        }

        val newEntity = view.camera.world.getEntityByID(entityId)
        if (newEntity == null) {
            LOGGER.warn("Oh no! The entity $entity with new id $entityId did not reappear at the other side of $this!")
            return
        }

        val pos = newEntity.pos
        val yaw = newEntity.rotationYaw
        val pitch = newEntity.rotationPitch
        Utils.transformPosition(entity, newEntity, this)
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
            LOGGER.warn("Failed to use portal $this because view has not been set")
            return false
        }
        UtilsClient.transformPosition(player, view.camera, this)

        val remotePortal = getRemotePortal()
        if (remotePortal == null) {
            LOGGER.warn("Failed to use portal $this because remote portal in $view couldn't be found")
            return false
        }

        view.makeMainView()
        Net.INSTANCE.sendToServer(UsePortal(entityId))

        remotePortal.onClientUpdate()
        return true
    }
}

/**
 * Suppresses the next fall damage a player will take (within 10 seconds).
 */
class PreventNextFallDamage(
        private val player: EntityPlayerMP
) {
    private var registered by MinecraftForge.EVENT_BUS
    /**
     * After this timeout reaches zero, we stop listening and assume the player somehow managed to not take fall damage.
     */
    private var timeoutTicks = 10 * 20 // 10 seconds

    init {
        registered = true
    }

    @SubscribeEvent
    fun onLivingFall(event: LivingFallEvent) {
        if (event.entity !== player) return // Note: cannot use != because Entity overwrites .equals
        event.isCanceled = true
        registered = false
    }

    @SubscribeEvent
    fun onTick(event: TickEvent.ServerTickEvent) {
        if (event.phase != TickEvent.Phase.START) return

        timeoutTicks--
        if (timeoutTicks <= 0) {
            registered = false
        }
    }
}

