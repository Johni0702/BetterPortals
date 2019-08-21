package de.johni0702.minecraft.betterportals.common.tile

import de.johni0702.minecraft.betterportals.common.Portal
import de.johni0702.minecraft.betterportals.common.PortalAccessor
import de.johni0702.minecraft.betterportals.common.PortalAgent
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.ResourceLocation
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

interface PortalTileEntity<out P: Portal.Mutable> {
    val agent: PortalAgent<P>?
}

class PortalTileEntityAccessor<E, P: Portal.Mutable>(
        private val type: Class<E>,
        private val world: World
) : PortalAccessor
        where E: PortalTileEntity<P>,
              E: TileEntity
{
    // Note: There's no nice way to implement [onChange] for tile entities (would probably require Mixins), so instead
    // (at least until it becomes necessary), this accessor will use the poll approach and makes sure to not
    // cause any allocations unless there's an actual change.
    private var updated = mutableListOf<E>()
    private var tileEntitiesList = mutableListOf<E>()
        get() {
            // loadedTileEntityList will usually be far longer (possibly five-digit) than tileEntityList (single-digit),
            // so rebuilding the latter should be more efficient than diffing
            updated.clear()
            world.loadedTileEntityList.forEach {
                if (type.isInstance(it)) {
                    updated.add(type.cast(it))
                }
            }
            if (updated != field) {
                // List changed, replace it instead of updating in-place because we've previously returned it, so
                // modifying it in-place could cause a CME if it happens to be iterated over atm.
                field = updated
                updated = mutableListOf()
            }
            return field
        }
    val tileEntities: List<E> get() = tileEntitiesList
    override val loadedPortals: Iterable<PortalAgent<Portal.Mutable>> =
            Sequence { tileEntities.iterator() }.mapNotNull { it.agent }.asIterable()

    override fun findById(id: ResourceLocation): PortalAgent<Portal.Mutable>? {
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