package de.johni0702.minecraft.betterportals.common.entity

import de.johni0702.minecraft.betterportals.common.*
import io.netty.buffer.ByteBuf
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.network.PacketBuffer
import net.minecraft.network.datasync.DataParameter
import net.minecraft.network.datasync.DataSerializers
import net.minecraft.network.datasync.EntityDataManager
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
import kotlin.properties.Delegates

open class PortalEntityPortalAgent<out E: AbstractPortalEntity>(
        manager: PortalManager,
        val entity: E,
        portalConfig: PortalConfiguration
) : PortalAgent<E>(
        manager,
        PortalEntityAccessor.getId(entity),
        entity,
        portalConfig
) {
    // The entity ID might not be correct before the entity is actually spawned into the world
    override val id: ResourceLocation
        get() = PortalEntityAccessor.getId(entity)
}

abstract class AbstractPortalEntity(
        world: World,
        override var plane: EnumFacing.Plane,
        relativeBlocks: Set<BlockPos>,
        override var localDimension: Int,
        localPosition: BlockPos,
        localRotation: Rotation,
        override var remoteDimension: Int?,
        remotePosition: BlockPos,
        remoteRotation: Rotation,
        portalConfig: PortalConfiguration
) : Entity(world), PortalEntity<FinitePortal.Mutable>, FinitePortal.Mutable, IEntityAdditionalSpawnData {

    companion object {
        private val PORTAL = EntityDataManager.createKey(AbstractPortalEntity::class.java, DataSerializers.COMPOUND_TAG)
    }

    override var localPosition: BlockPos by Delegates.observable(localPosition) { _, _, _ -> updatePortalFields() }
    override var localRotation: Rotation by Delegates.observable(localRotation) { _, _, _ -> updatePortalFields() }
    override var remotePosition: BlockPos by Delegates.observable(remotePosition) { _, _, _ -> updatePortalFields() }
    override var remoteRotation: Rotation by Delegates.observable(remoteRotation) { _, _, _ -> updatePortalFields() }
    override var relativeBlocks: Set<BlockPos> by Delegates.observable(relativeBlocks) { _, _, _ -> updatePortalFields() }

    private lateinit var _localBlocks: Set<BlockPos>
    private lateinit var _remoteBlocks: Set<BlockPos>

    private lateinit var _localDetailedBounds: List<AxisAlignedBB>
    private lateinit var _remoteDetailedBounds: List<AxisAlignedBB>

    private lateinit var _localBoundingBox: AxisAlignedBB
    private lateinit var _remoteBoundingBox: AxisAlignedBB

    override val localBlocks: Set<BlockPos> get() = _localBlocks
    override val remoteBlocks: Set<BlockPos> get() = _remoteBlocks

    override val localDetailedBounds: List<AxisAlignedBB> get() = _localDetailedBounds
    override val remoteDetailedBounds: List<AxisAlignedBB> get() = _remoteDetailedBounds

    override val localBoundingBox: AxisAlignedBB get() = _localBoundingBox
    override val remoteBoundingBox: AxisAlignedBB get() = _remoteBoundingBox

    private fun updatePortalFields() {
        _localBlocks = relativeBlocks.map { it.toLocal() }.toSet()
        _remoteBlocks = relativeBlocks.map { it.toRemote() }.toSet()

        _localDetailedBounds = localBlocks.map(::AxisAlignedBB)
        _remoteDetailedBounds = remoteBlocks.map(::AxisAlignedBB)

        _localBoundingBox = localBlocks.toAxisAlignedBB()
        _remoteBoundingBox = remoteBlocks.toAxisAlignedBB()

        with(localPosition.to3d()) { setPosition(x + 0.5, y + 0.5, z + 0.5) }
        setRotation(localRotation.degrees.toFloat(), 0f)
        dataManager[PORTAL] = writePortalToNBT()
    }

    init {
        updatePortalFields()
    }

    override fun getRenderBoundingBox(): AxisAlignedBB = localBoundingBox

    override val agent = PortalEntityPortalAgent(world.portalManager, this, portalConfig)

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
        dataManager.register(PORTAL, NBTTagCompound())
    }

    override fun notifyDataManagerChange(key: DataParameter<*>) {
        super.notifyDataManagerChange(key)
        if (world.isRemote) {
            readPortalFromNBT(dataManager.get(PORTAL))
        }
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
            agent.remoteWorld ?: return null
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

    private val trackingPlayers = mutableListOf<EntityPlayerMP>()

    override fun addTrackingPlayer(player: EntityPlayerMP) {
        super.addTrackingPlayer(player)
        trackingPlayers.add(player)
        agent.addTrackingPlayer(player)
    }

    override fun removeTrackingPlayer(player: EntityPlayerMP) {
        super.removeTrackingPlayer(player)
        trackingPlayers.remove(player)
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
    }
}
