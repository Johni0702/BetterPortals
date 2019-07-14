package de.johni0702.minecraft.betterportals.impl.mekanism.common.tile

import de.johni0702.minecraft.betterportals.common.FinitePortal
import de.johni0702.minecraft.betterportals.common.PortalAgent
import de.johni0702.minecraft.betterportals.common.portalManager
import de.johni0702.minecraft.betterportals.common.tile.PortalTileEntity
import de.johni0702.minecraft.betterportals.common.tile.PortalTileEntityAccessor
import de.johni0702.minecraft.betterportals.common.toRotation
import de.johni0702.minecraft.betterportals.impl.mekanism.common.LOGGER
import de.johni0702.minecraft.betterportals.impl.mekanism.common.compareTo
import de.johni0702.minecraft.view.server.FixedLocationTicket
import mekanism.api.Coord4D
import mekanism.common.Mekanism
import mekanism.common.config.MekanismConfig
import mekanism.common.network.PacketPortalFX
import mekanism.common.tile.TileEntityTeleporter
import mekanism.common.util.MekanismUtils
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.init.SoundEvents
import net.minecraft.util.EnumFacing
import net.minecraft.util.Rotation
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import net.minecraft.world.WorldServer

private val RELATIVE_PORTAL_BLOCKS: Set<BlockPos> = setOf(BlockPos.ORIGIN, BlockPos.ORIGIN.up())

data class LinkedTeleporterPortal(
        override var localDimension: Int,
        override var localPosition: BlockPos,
        override var localRotation: Rotation,
        override var remoteDimension: Int?,
        override var remotePosition: BlockPos,
        override var remoteRotation: Rotation
) : FinitePortal.Mutable {
    override var plane: EnumFacing.Plane = EnumFacing.Plane.VERTICAL
    override var relativeBlocks: Set<BlockPos> = RELATIVE_PORTAL_BLOCKS

    fun toRemote(): LinkedTeleporterPortal = LinkedTeleporterPortal(
            remoteDimension!!, remotePosition, remoteRotation,
            localDimension, localPosition, localRotation
    )
}

class TeleporterPortalAgent(
        val tileEntity: TileEntityBetterTeleporter,
        portal: LinkedTeleporterPortal
) : PortalAgent<FixedLocationTicket, LinkedTeleporterPortal>(
        tileEntity.world.portalManager,
        PortalTileEntityAccessor.getId(tileEntity),
        portal,
        { it.allocateFixedLocationTicket() }
) {
    override fun modifyAABBs(entity: Entity, queryAABB: AxisAlignedBB, aabbList: MutableList<AxisAlignedBB>, queryRemote: (World, AxisAlignedBB) -> List<AxisAlignedBB>) {
        if (getRemoteAgent() == null) {
            // Remote portal hasn't yet been loaded, act as if the portal wasn't there (as opposed to solid)
            return
        }
        super.modifyAABBs(entity, queryAABB, aabbList, queryRemote)
    }

    override fun teleport(entity: Entity, from: EnumFacing) {
        val remote = getRemoteAgent() ?: return
        if (!tileEntity.active) {
            return
        }
        if (!world.isRemote && entity !is EntityPlayer) {
            val energyCost = tileEntity.calculateEnergyCost(Coord4D.get(tileEntity), Coord4D(portal.remotePosition, remote.world))
            if (tileEntity.energy < energyCost) {
                return
            }
            tileEntity.energy -= energyCost

            tileEntity.playTeleportEffects(entity)
        }
        super.teleport(entity, from)
    }

    override fun serverPortalUsed(player: EntityPlayerMP): Boolean {
        if (!MekanismUtils.canFunction(tileEntity)) {
            LOGGER.warn("Player used teleporter $this which was disabled, resetting player..")
            player.setPositionAndUpdate(player.posX, player.posY, player.posZ)
            return false
        }

        val remote = getRemoteAgent() as? TeleporterPortalAgent ?: return false
        val energyCost = tileEntity.calculateEnergyCost(Coord4D.get(tileEntity), Coord4D(portal.remotePosition, remote.world))
        if (tileEntity.energy < energyCost) {
            LOGGER.warn("Player used teleporter $this which has insufficient energy, resetting player..")
            player.setPositionAndUpdate(player.posX, player.posY, player.posZ)
            return false
        }
        tileEntity.energy -= energyCost

        tileEntity.playTeleportEffects(player)

        tileEntity.syncWatchers(remote.world as WorldServer)
        remote.tileEntity.syncWatchers(remote.world as WorldServer)
        return if (super.serverPortalUsed(player)) {
            tileEntity.syncWatchers(remote.world as WorldServer, updateAgent = false)
            remote.tileEntity.syncWatchers(remote.world as WorldServer, updateAgent = false)
            true
        } else {
            false
        }
    }
}

class TileEntityBetterTeleporter : TileEntityTeleporter(), PortalTileEntity<LinkedTeleporterPortal> {
    override var agent: TeleporterPortalAgent? = null
    private val trackingPlayers = mutableListOf<EntityPlayerMP>()
    val active get() = shouldRender && MekanismUtils.canFunction(this)

    override fun setPos(posIn: BlockPos) {
        super.setPos(posIn)
        if (world.isRemote) {
            val localCoord = Coord4D.get(this)
            val portal = LinkedTeleporterPortal(
                    world.provider.dimension,
                    localCoord.pos.up(),
                    Rotation.NONE,
                    null,
                    BlockPos.ORIGIN,
                    Rotation.NONE
            )
            agent = TeleporterPortalAgent(this, portal)
        }
    }

    private val portalFacing get() = Coord4D.get(this).step(EnumFacing.UP).let {bottomPortalBlock ->
        EnumFacing.HORIZONTALS.find { bottomPortalBlock.step(it).isAirBlock(world) } ?: EnumFacing.NORTH
    }

    private fun getPortal(): Pair<LinkedTeleporterPortal, TileEntityBetterTeleporter>? {
        if (!hasFrame()) {
            return null
        }

        val localCoord = Coord4D.get(this)!!
        val remoteCoord = closest ?: return null

        val remoteWorld = world.minecraftServer!!.getWorld(remoteCoord.dimensionId) ?: return null
        val remoteTileEntity = remoteWorld.getTileEntity(remoteCoord.pos) as? TileEntityBetterTeleporter ?: return null

        var localFacing = portalFacing
        var remoteFacing = remoteTileEntity.portalFacing

        // Flip one (arbitrary but always the same) of the two portal facings
        if (localCoord < remoteCoord) {
            localFacing = localFacing.opposite
        } else {
            remoteFacing = remoteFacing.opposite
        }

        val portal = LinkedTeleporterPortal(
                localCoord.dimensionId,
                localCoord.pos.up(),
                localFacing.toRotation(),
                remoteCoord.dimensionId,
                remoteCoord.pos.up(),
                remoteFacing.toRotation()
        )
        return Pair(portal, remoteTileEntity)
    }

    override fun getClosest(): Coord4D? {
        val frequency = frequency ?: return null
        val localCoord = Coord4D.get(this)!!
        val remoteCoord = frequency.getClosestCoords(localCoord) ?: return null
        if (frequency.getClosestCoords(remoteCoord) != localCoord) {
            return null // require portals to link in pairs
        }
        return remoteCoord
    }

    private fun link(remote: TileEntityBetterTeleporter, portal: LinkedTeleporterPortal) {
        this.destroyAgent()
        remote.destroyAgent()

        this.agent = TeleporterPortalAgent(this, portal)
        remote.agent = TeleporterPortalAgent(remote, portal.toRemote())

        this.trackingPlayers.forEach { this.agent?.addTrackingPlayer(it) }
        remote.trackingPlayers.forEach { remote.agent?.addTrackingPlayer(it) }
    }

    private fun destroyAgent() {
        val agent = this.agent ?: return
        val remoteAgent = agent.getRemoteAgent() as? TeleporterPortalAgent

        trackingPlayers.forEach { agent.removeTrackingPlayer(it) }
        this.agent = null

        remoteAgent?.tileEntity?.destroyAgent()
    }

    fun syncWatchers(world: WorldServer, updateAgent: Boolean = true) {
        val actualWatchers = world.playerChunkMap.getEntry(pos.x shr 4, pos.z shr 4)?.watchingPlayers ?: listOf()
        trackingPlayers.removeIf {
            if (actualWatchers.contains(it)) {
                false
            } else {
                if (updateAgent) {
                    agent?.removeTrackingPlayer(it)
                }
                true
            }
        }
        actualWatchers.forEach {
            if (!trackingPlayers.contains(it)) {
                trackingPlayers.add(it)
                if (updateAgent) {
                    agent?.addTrackingPlayer(it)
                }
            }
        }
    }

    override fun onUpdate() {
        super.onUpdate()

        val world = world
        if (world is WorldServer) {
            val portal = getPortal()
            if (agent?.portal != portal?.first) {
                destroyAgent()
            }
            if (portal != null && agent == null) {
                link(portal.second, portal.first)
            }

            syncWatchers(world)
        }
    }

    fun playTeleportEffects(entity: Entity) {
        for (coord in frequency.activeCoords) {
            Mekanism.packetHandler.sendToAllAround(PacketPortalFX.PortalFXMessage(coord), coord.getTargetPoint(40.0))
        }
        world.playSound(null, entity.posX, entity.posY, entity.posZ, SoundEvents.ENTITY_ENDERMEN_TELEPORT, entity.soundCategory, 1.0F, 1.0F)
    }

    fun calculateEnergyCost(from: Coord4D, to: Coord4D): Int {
        val config = MekanismConfig.current().usage
        return config.teleporterBase.`val`() + if (from.dimensionId != to.dimensionId) {
            config.teleporterDimensionPenalty.`val`()
        } else {
            from.distanceTo(to) * config.teleporterDistance.`val`()
        }
    }

    override fun canTeleport(): Byte {
        return when (val result = super.canTeleport()) {
            1.toByte() -> if (energy < calculateEnergyCost(Coord4D.get(this), closest!!)) 4 else 1
            else -> result
        }

    }

    // Disable normal teleport behavior
    override fun teleport() = Unit
    override fun getToTeleport(): MutableList<Entity> = mutableListOf()
}