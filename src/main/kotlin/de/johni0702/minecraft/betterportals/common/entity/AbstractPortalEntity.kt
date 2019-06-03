package de.johni0702.minecraft.betterportals.common.entity

import de.johni0702.minecraft.betterportals.client.deriveClientPosRotFrom
import de.johni0702.minecraft.betterportals.common.*
import de.johni0702.minecraft.view.server.FixedLocationTicket
import io.netty.buffer.ByteBuf
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
