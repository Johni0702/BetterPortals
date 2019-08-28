package de.johni0702.minecraft.betterportals.impl.travelhuts.common.entity

import de.johni0702.minecraft.betterportals.common.*
import de.johni0702.minecraft.betterportals.common.entity.AbstractPortalEntity
import de.johni0702.minecraft.betterportals.common.entity.PortalEntityPortalAgent
import de.johni0702.minecraft.betterportals.impl.travelhuts.common.TRAVELHUTS_PORTAL_CONFIG
import de.johni0702.minecraft.view.server.SimpleView
import de.johni0702.minecraft.view.server.View
import de.johni0702.minecraft.view.server.worldsManager
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.util.EnumFacing
import net.minecraft.world.World
import net.minecraft.world.WorldServer

open class TravelHutsPortalAgent(manager: PortalManager) : PortalEntityPortalAgent(manager, TRAVELHUTS_PORTAL_CONFIG) {
    // TravelHuts portals have a back plane of 1/16 width
    override fun getClippingPlaneOffset(cameraSide: EnumFacing): Double = 0.5 - 1.0 / 16.0

    override fun registerView(player: EntityPlayerMP): View? {
        val remoteWorld = remoteWorld as WorldServer? ?: return null
        val anchor = Pair(world as WorldServer, portal.localPosition.toCubePos())
        val manager = player.worldsManager
        return SimpleView(manager, remoteWorld, portal.remotePosition.to3dMid(), anchor, 4).also {
            manager.registerView(it)
        }
    }
}

class TravelHutsPortalEntity(
        world: World,
        portal: FinitePortal
) : AbstractPortalEntity(
        world,
        portal,
        TravelHutsPortalAgent(world.portalManager)
) {
    @Suppress("unused")
    constructor(world: World) : this(world, FinitePortal.DUMMY)
}