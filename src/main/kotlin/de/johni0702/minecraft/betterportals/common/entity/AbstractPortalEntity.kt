package de.johni0702.minecraft.betterportals.common.entity

import de.johni0702.minecraft.betterportals.client.deriveClientPosRotFrom
import de.johni0702.minecraft.betterportals.common.*
import de.johni0702.minecraft.view.server.FixedLocationTicket
import io.netty.buffer.ByteBuf
import net.minecraft.block.material.Material
import net.minecraft.client.Minecraft
import net.minecraft.client.entity.EntityPlayerSP
import net.minecraft.client.renderer.culling.ICamera
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.network.PacketBuffer
import net.minecraft.util.EnumFacing
import net.minecraft.util.ResourceLocation
import net.minecraft.util.Rotation
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import net.minecraft.world.WorldServer
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.world.GetCollisionBoxesEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly

open class PortalEntityPortalAgent(
        manager: PortalManager,
        open val entity: AbstractPortalEntity
) : PortalAgent<FixedLocationTicket>(
        manager,
        PortalEntityAccessor.getId(entity),
        entity,
        { it.allocateFixedLocationTicket() }
) {
    // The entity ID might not be correct before the entity is actually spawned into the world
    override val id: ResourceLocation
        get() = PortalEntityAccessor.getId(entity)

    override fun serverPortalUsed(player: EntityPlayerMP): Boolean {
        val remotePortal = entity.getRemotePortal()
        entity.ignoreTracking = true
        remotePortal?.ignoreTracking = true
        try {
            return super.serverPortalUsed(player)
        } finally {
            entity.ignoreTracking = false
            remotePortal?.ignoreTracking = false
        }
    }
}

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
) : Entity(world), PortalEntity, FinitePortal.Mutable, IEntityAdditionalSpawnData {

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

    override val agent = PortalEntityPortalAgent(world.portalManager, this)

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
            world.getEntities(AbstractPortalEntity::class.java, { it?.isDead == false }).forEach {
                it.agent.checkTeleportees()
            }
        }

        @SubscribeEvent(priority = EventPriority.LOW)
        fun onGetCollisionBoxes(event: GetCollisionBoxesEvent) {
            val entity = event.entity ?: collisionBoxesEntity ?: return
            modifyAABBs(entity, event.aabb, event.aabb, event.collisionBoxesList) { world, aabb ->
                world.getCollisionBoxes(null, aabb)
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
            val world = entity.world
            world.getEntities(AbstractPortalEntity::class.java) { it?.isDead == false }.forEach { portal ->
                if (!portal.localBoundingBox.intersects(entityAABB)) return@forEach // not even close

                val remotePortal = portal.getRemotePortal()
                if (remotePortal == null) {
                    // Remote portal hasn't yet been loaded, treat all portal blocks as solid to prevent passing
                    portal.localBlocks.forEach {
                        val blockAABB = AxisAlignedBB(it)
                        if (blockAABB.intersects(entityAABB)) {
                            aabbList.add(blockAABB)
                        }
                    }
                    return@forEach
                }

                // If this is a non-rectangular portal and the entity isn't inside it, we don't care
                if (portal.localBlocks.none { AxisAlignedBB(it).intersects(entityAABB) }) return@forEach

                // otherwise, we need to remove all collision boxes on the other, local side of the portal
                // to prevent the entity from colliding with them
                val entitySide = portal.agent.getEntitySide(entity)
                val hiddenSide = entitySide.opposite
                val hiddenAABB = portal.localBoundingBox
                        .offset(hiddenSide.directionVec.to3d())
                        .expand(hiddenSide.directionVec.to3d() * Double.POSITIVE_INFINITY)
                aabbList.removeIf { it.intersects(hiddenAABB) }

                // and instead add collision boxes from the remote world
                if (!hiddenAABB.intersects(queryAABB)) return@forEach // unless we're not even interested in those
                val remoteAABB = with(portal) {
                    // Reduce the AABB which we're looking for in the first place to the hidden section
                    val aabb = hiddenAABB.intersect(queryAABB)
                    // and transform it to remote space in order to lookup collision boxes over there
                    aabb.min.fromLocal().toRemote().toAxisAlignedBB(aabb.max.fromLocal().toRemote())
                }
                // Unset the entity while calling into the remote world since it's not valid over there
                collisionBoxesEntity = collisionBoxesEntity.also {
                    collisionBoxesEntity = null
                    val remoteCollisions = queryRemote(remotePortal.world, remoteAABB)

                    // finally transform any collision boxes back to local space and add them to the result
                    remoteCollisions.mapTo(aabbList) { aabb ->
                        with(portal) { aabb.min.fromRemote().toLocal().toAxisAlignedBB(aabb.max.fromRemote().toLocal()) }
                    }
                }
            }
        }

        fun isInMaterial(entity: Entity, material: Material): Boolean? {
            val entityAABB = entity.entityBoundingBox
            val queryAABB = entityAABB.grow(-0.1, -0.4, -0.1)

            val world = entity.world
            world.getEntities(AbstractPortalEntity::class.java) { it?.isDead == false }.forEach { portal ->
                if (!portal.localBoundingBox.intersects(entityAABB)) return@forEach // not even close

                val remotePortal = portal.getRemotePortal() ?: return@forEach

                // If this is a non-rectangular portal and the entity isn't inside it, we don't care
                if (portal.localBlocks.none { AxisAlignedBB(it).intersects(entityAABB) }) return@forEach

                val portalPos = portal.localPosition.to3dMid()
                val entitySide = portal.agent.getEntitySide(entity)
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

            // Entity not in any portal, fallback to default implementation
            return null
        }
    }

    fun getRemotePortal(): AbstractPortalEntity? {
        val remoteWorld = if (world.isRemote) {
            (agent.view ?: return null).camera.world
        } else {
            world.minecraftServer!!.getWorld(remoteDimension ?: return null)
        }
        val chunk = remoteWorld.getChunkFromBlockCoords(remotePosition)
        val list = mutableListOf<AbstractPortalEntity>()
        chunk.getEntitiesOfTypeWithinAABB(javaClass, AxisAlignedBB(remotePosition), list) {
            it?.agent?.isLinked(agent) == true
        }
        return list.firstOrNull()
    }

    //
    //  Server-side
    //

    // When switching main view of a player, we need to ignore any add/removeTrackingPlayer calls
    // as otherwise we'll release our ticket for the player and might not be able to get it back.
    internal var ignoreTracking = false
    private val trackingPlayers = mutableListOf<EntityPlayerMP>()

    override fun addTrackingPlayer(player: EntityPlayerMP) {
        super.addTrackingPlayer(player)

        trackingPlayers.add(player)

        if (ignoreTracking) return

        agent.addTrackingPlayer(player)
    }

    override fun removeTrackingPlayer(player: EntityPlayerMP) {
        super.removeTrackingPlayer(player)

        trackingPlayers.remove(player)

        if (ignoreTracking) return

        agent.removeTrackingPlayer(player)
    }

    override fun link(other: Portal.Linkable) {
        if (this.remoteDimension != null) {
            // Unlink all tracking players
            trackingPlayers.toList().forEach {
                removeTrackingPlayer(it)
            }
        }

        super.link(other)

        // Update tracking players
        trackingPlayers.toList().forEach {
            addTrackingPlayer(it)
        }
        getRemotePortal()?.let { remotePortal ->
            remotePortal.trackingPlayers.toList().forEach {
                remotePortal.addTrackingPlayer(it)
            }
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
    override fun isInRangeToRenderDist(distance: Double): Boolean = true // MC makes this depend on entityBoundingBox

    @SideOnly(Side.CLIENT)
    protected open fun onClientUpdate() {
        val player = world.getPlayers(EntityPlayerSP::class.java) { true }[0]
        agent.view?.let { view ->
            if (!view.isMainView) {
                view.camera.deriveClientPosRotFrom(player, this)
            }
        }
    }

    @SideOnly(Side.CLIENT)
    open fun canBeSeen(camera: ICamera): Boolean =
            camera.isBoundingBoxInFrustum(renderBoundingBox)
                    && localBlocks.any { camera.isBoundingBoxInFrustum(AxisAlignedBB(it)) }
}
