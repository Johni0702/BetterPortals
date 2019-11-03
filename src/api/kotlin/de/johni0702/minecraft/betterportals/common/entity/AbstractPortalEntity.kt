package de.johni0702.minecraft.betterportals.common.entity

import de.johni0702.minecraft.betterportals.common.*
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.init.Blocks
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.network.Packet
import net.minecraft.network.datasync.DataParameter
import net.minecraft.network.datasync.DataSerializers
import net.minecraft.network.datasync.EntityDataManager
import net.minecraft.util.ResourceLocation
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.world.World
import net.minecraft.world.WorldServer
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly

//#if MC>=11400
//$$ import net.minecraft.entity.EntityType
//#if FABRIC>=1
//$$ import de.johni0702.minecraft.betterportals.impl.theImpl
//$$ import net.minecraft.client.network.packet.EntitySpawnS2CPacket
//#else
//$$ import net.minecraftforge.fml.network.NetworkHooks
//#endif
//#endif

interface Linkable {
    val portal: FinitePortal
    fun link(other: Linkable)
}

open class PortalEntityPortalAgent(
        manager: PortalManager,
        portalConfig: PortalConfiguration
) : PortalAgent<FinitePortal>(
        manager,
        ResourceLocation(""),
        FinitePortal.DUMMY,
        portalConfig
) {
    lateinit var entity: AbstractPortalEntity
        internal set

    // The entity ID might not be correct before the entity is actually spawned into the world
    override val id: ResourceLocation
        get() = PortalEntityAccessor.getId(entity)
}

abstract class AbstractPortalEntity(
        //#if MC>=11400
        //$$ type: EntityType<out AbstractPortalEntity>,
        //#endif
        world: World,
        portal: FinitePortal,
        final override val agent: PortalEntityPortalAgent
) : Entity(
        //#if MC>=11400
        //$$ type,
        //#endif
        world
), PortalEntity, Linkable {
    constructor(
            //#if MC>=11400
            //$$ type: EntityType<out AbstractPortalEntity>,
            //#endif
            world: World, portal: FinitePortal, portalConfig: PortalConfiguration
    ) : this(
            //#if MC>=11400
            //$$ type,
            //#endif
            world, portal, PortalEntityPortalAgent(world.portalManager, portalConfig)
    )

    companion object {
        private val PORTAL: DataParameter<NBTTagCompound> = EntityDataManager.createKey(AbstractPortalEntity::class.java, DataSerializers.COMPOUND_TAG)
    }

    init {
        @Suppress("LeakingThis")
        agent.entity = this
        agent.portal = portal
        dataManager[PORTAL] = portal.writePortalToNBT()
    }

    override var portal: FinitePortal = portal
        set(value) {
            field = value
            dataManager[PORTAL] = value.writePortalToNBT()
            agent.portal = value
            with(value.localPosition.to3dMid()) { setPosition(x, y, z) }
        }

    override fun getRenderBoundingBox(): AxisAlignedBB = portal.localBoundingBox

    init {
        // MC checks whether entities are completely inside the view frustum which is completely useless/broken for
        // large entities.
        // The sane solution would have been to check if they are partially inside it.
        // If required for performance, that check may be implemented in Render#shouldRender but I'm not doing it now.
        // I blame MC
        ignoreFrustumCheck = true

        //#if MC<11400
        width = 0f
        height = 0f
        //#endif

        with(portal.localPosition.to3d()) { setPosition(x + 0.5, y + 0.5, z + 0.5) }
        this.setRotation(portal.localRotation.degrees.toFloat(), 0f)
    }

    override fun entityInit() {
        dataManager.register(PORTAL, NBTTagCompound())
    }

    override fun notifyDataManagerChange(key: DataParameter<*>) {
        super.notifyDataManagerChange(key)
        if (world.isRemote && key == PORTAL) {
            portal = FinitePortal(dataManager.get(PORTAL))
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
        portal = FinitePortal(compound.getTag("BetterPortal") as NBTTagCompound)
    }

    override fun writeEntityToNBT(compound: NBTTagCompound) {
        compound.setTag("BetterPortal", portal.writePortalToNBT())
    }

    //#if FABRIC>=1
    //$$ override fun createSpawnPacket(): Packet<*> = theImpl.createSpawnPacket(this)
    //#else
    //#if MC>=11400
    //$$ override fun createSpawnPacket(): IPacket<*> = NetworkHooks.getEntitySpawningPacket(this)
    //#endif
    //#endif

    fun getRemotePortal(): AbstractPortalEntity? {
        val remoteWorld = if (world.isRemote) {
            agent.remoteClientWorld ?: return null
        } else {
            world.minecraftServer!!.getWorld(portal.remoteDimension ?: return null)
        }
        val chunk = remoteWorld.getChunkFromBlockCoords(portal.remotePosition)
        val list = mutableListOf<AbstractPortalEntity>()
        // FIXME preprocessor doesn't currently remap this one for yet-to-be-determined reason
        //#if FABRIC>=1
        //$$ chunk.appendEntities(javaClass, Box(portal.remotePosition), list) {
        //#else
        chunk.getEntitiesOfTypeWithinAABB(javaClass, AxisAlignedBB(portal.remotePosition), list) {
        //#endif
            it?.agent?.isLinked(agent) == true
        }
        return list.firstOrNull()
    }

    //
    //  Server-side
    //

    override fun addTrackingPlayer(player: EntityPlayerMP) {
        super.addTrackingPlayer(player)
        agent.addTrackingPlayer(player)
    }

    override fun removeTrackingPlayer(player: EntityPlayerMP) {
        super.removeTrackingPlayer(player)
        agent.removeTrackingPlayer(player)
    }

    override fun link(other: Linkable) {
        portal = portal.withRemote(other.portal)
        if (!other.portal.isTarget(portal)) {
            other.link(this)
        }
        // Need to update views after the remote is linked as well
        agent.updateViews()
    }

    override fun setDead() {
        if (!isEntityAlive) return
        super.setDead()
        if (world is WorldServer) {
            getRemotePortal()?.setDead()
            removePortal()
        }
    }

    protected open fun removePortal() {
        portal.localBlocks.forEach { world.setBlockState(it, Blocks.AIR.defaultState) }
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
