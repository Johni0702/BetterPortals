package de.johni0702.minecraft.betterportals.common.tile

import de.johni0702.minecraft.betterportals.common.Portal
import de.johni0702.minecraft.betterportals.common.PortalAccessor
import de.johni0702.minecraft.betterportals.common.PortalAgent
import de.johni0702.minecraft.view.server.FixedLocationTicket
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.ResourceLocation
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

interface PortalTileEntity<out P: Portal.Mutable> {
    val agent: PortalAgent<FixedLocationTicket, P>?
}

class PortalTileEntityAccessor<E, P: Portal.Mutable>(
        private val type: Class<E>,
        private val world: World
) : PortalAccessor<FixedLocationTicket>
        where E: PortalTileEntity<P>,
              E: TileEntity
{
    val tileEntities: List<E>
        get() = world.loadedTileEntityList.mapNotNull { if (type.isInstance(it)) type.cast(it) else null }
    override val loadedPortals: Iterable<PortalAgent<FixedLocationTicket, Portal.Mutable>>
        get() = tileEntities.mapNotNull { it.agent }

    override fun findById(id: ResourceLocation): PortalAgent<FixedLocationTicket, Portal.Mutable>? {
        if (id.resourceDomain != "minecraft") return null
        if (!id.resourcePath.startsWith("tile/pos/")) return null
        val pos = id.resourcePath.substring("tile/pos/".length).toBlockPos() ?: return null
        val tileEntity = world.getTileEntity(pos)
        if (!type.isInstance(tileEntity)) return null
        return type.cast(tileEntity)?.agent
    }

    companion object {
        fun <E> getId(entity: E): ResourceLocation
                where E: TileEntity,
                      E: PortalTileEntity<*> = ResourceLocation("minecraft", with(entity.pos) { "tile/pos/$x/$y/$z"})
    }

    private fun String.toBlockPos(): BlockPos? {
        val parts = split("/", limit = 3)
        if (parts.size != 3) return null
        val (x, y, z) = parts.map { it.toIntOrNull() ?: return null }
        return BlockPos(x, y, z)
    }
}