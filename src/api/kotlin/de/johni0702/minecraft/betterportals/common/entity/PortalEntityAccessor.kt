package de.johni0702.minecraft.betterportals.common.entity

import de.johni0702.minecraft.betterportals.common.Portal
import de.johni0702.minecraft.betterportals.common.PortalAccessor
import de.johni0702.minecraft.betterportals.common.PortalAgent
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.ResourceLocation
import net.minecraft.util.SoundCategory
import net.minecraft.util.SoundEvent
import net.minecraft.util.math.BlockPos
import net.minecraft.world.IWorldEventListener
import net.minecraft.world.World

interface PortalEntity<out P: Portal.Mutable> {
    val agent: PortalAgent<P>

    interface OneWay<out P: Portal.Mutable> : PortalEntity<P> {
        /**
         * Whether this portal instance is the tail/exit end of a pair of portals.
         * Not to be confused with the exit portal which spawns after the dragon fight; its tail end is in the overworld.
         * A pair of one-way portals cannot be entered from the tail end.
         */
        val isTailEnd: Boolean

        /**
         * Whether the tail end of the pair of portals is currently visible. Ignored if [isTailEnd] is false.
         * Ignored on the server.
         *
         * The tail end of a one-way portal pair will usually disappear shortly after you've used it.
         */
        val isTailEndVisible: Boolean
    }
}

class PortalEntityAccessor<E, P: Portal.Mutable>(
        private val type: Class<E>,
        private val world: World
) : PortalAccessor
        where E: PortalEntity<P>,
              E: Entity
{
    val entities: List<E>
        get() = world.getEntities(type) { it?.isDead == false }
    override val loadedPortals: Iterable<PortalAgent<Portal.Mutable>>
        get() = entities.map { it.agent }

    private val changeCallbacks = mutableListOf<() -> Unit>()
    init {
        world.addEventListener(object : IWorldEventListener {
            override fun onEntityAdded(entity: Entity) {
                if (type.isInstance(entity)) {
                    changeCallbacks.forEach { it() }
                }
            }

            override fun onEntityRemoved(entity: Entity) {
                if (type.isInstance(entity)) {
                    changeCallbacks.forEach { it() }
                }
            }

            override fun playSoundToAllNearExcept(player: EntityPlayer?, soundIn: SoundEvent?, category: SoundCategory?, x: Double, y: Double, z: Double, volume: Float, pitch: Float) = Unit
            override fun broadcastSound(soundID: Int, pos: BlockPos?, data: Int) = Unit
            override fun playEvent(player: EntityPlayer?, type: Int, blockPosIn: BlockPos?, data: Int) = Unit
            override fun markBlockRangeForRenderUpdate(x1: Int, y1: Int, z1: Int, x2: Int, y2: Int, z2: Int) = Unit
            override fun notifyLightSet(pos: BlockPos?) = Unit
            override fun spawnParticle(particleID: Int, ignoreRange: Boolean, xCoord: Double, yCoord: Double, zCoord: Double, xSpeed: Double, ySpeed: Double, zSpeed: Double, vararg parameters: Int) = Unit
            override fun spawnParticle(id: Int, ignoreRange: Boolean, p_190570_3_: Boolean, x: Double, y: Double, z: Double, xSpeed: Double, ySpeed: Double, zSpeed: Double, vararg parameters: Int) = Unit
            override fun notifyBlockUpdate(worldIn: World?, pos: BlockPos?, oldState: IBlockState?, newState: IBlockState?, flags: Int) = Unit
            override fun playRecord(soundIn: SoundEvent?, pos: BlockPos?) = Unit
            override fun sendBlockBreakProgress(breakerId: Int, pos: BlockPos?, progress: Int) = Unit
        })
    }

    override fun findById(id: ResourceLocation): PortalAgent<Portal.Mutable>? {
        if (id.resourceDomain != "minecraft") return null
        if (!id.resourcePath.startsWith("entity/id/")) return null
        val entityId = id.resourcePath.substring("entity/id/".length).toIntOrNull() ?: return null
        val entity = world.getEntityByID(entityId)
        if (!type.isInstance(entity)) return null
        return type.cast(world.getEntityByID(entityId))?.agent
    }

    override fun onChange(callback: () -> Unit): Boolean {
        changeCallbacks.add(callback)
        return true
    }

    companion object {
        fun <E> getId(entity: E): ResourceLocation
                where E: Entity,
                      E: PortalEntity<*> = ResourceLocation("minecraft", "entity/id/" + entity.entityId)
    }
}