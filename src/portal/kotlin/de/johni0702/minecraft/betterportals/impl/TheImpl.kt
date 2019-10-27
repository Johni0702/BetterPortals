package de.johni0702.minecraft.betterportals.impl

import de.johni0702.minecraft.betterportals.common.BetterPortalsAPI
import net.minecraft.entity.Entity
import net.minecraft.util.ResourceLocation
import net.minecraft.world.World
import net.minecraftforge.registries.ObjectHolderRegistry

//#if MC>=11400
//$$ import de.johni0702.minecraft.betterportals.common.provideDelegate
//$$ import de.johni0702.minecraft.betterportals.impl.mixin.AccessorChunkManager
//$$ import net.minecraft.entity.player.ServerPlayerEntity
//$$ import net.minecraft.util.math.RayTraceContext
//$$ import net.minecraft.util.math.SectionPos
//$$ import net.minecraft.util.math.Vec3d
//$$ import net.minecraft.world.server.ServerWorld
//#else
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.SoundCategory
import net.minecraft.util.SoundEvent
import net.minecraft.util.math.BlockPos
import net.minecraft.world.IWorldEventListener
//#endif

//#if MC>=11400
//$$ interface EntityEventEmitter {
//$$     fun addEntitiesListener(onEntityAdded: (Entity) -> Unit, onEntityRemoved: (Entity) -> Unit)
//$$ }
//#endif

//#if MC<11400
interface IObjectHolderRegistry {
    fun addHandler(handler: () -> Unit)
}
//#endif

object TheImpl : Impl {
    override val portalApi: BetterPortalsAPI = BetterPortalsAPIImpl

    override fun World.addEntitiesListener(onEntityAdded: (Entity) -> Unit, onEntityRemoved: (Entity) -> Unit) {
        //#if MC>=11400
        //$$ (this as EntityEventEmitter).addEntitiesListener(onEntityAdded, onEntityRemoved)
        //#else
        addEventListener(object : IWorldEventListener {
            override fun onEntityAdded(entity: Entity) {
                onEntityAdded(entity)
            }

            override fun onEntityRemoved(entity: Entity) {
                onEntityRemoved(entity)
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
        //#endif
    }

    override fun addObjectHolderHandler(handler: (filter: (ResourceLocation) -> Boolean) -> Unit) {
        //#if MC>=11400
        //$$ ObjectHolderRegistry.addHandler { filter -> handler { filter.test(it) }  }
        //#else
        (ObjectHolderRegistry.INSTANCE as IObjectHolderRegistry).addHandler { handler { true } }
        //#endif
    }

    //#if MC>=11400
    //$$ private fun ServerWorld.getTracker(entity: Entity) =
    //$$         (chunkProvider.chunkManager as AccessorChunkManager).entities.get(entity.entityId)!!
    //$$
    //$$ override fun ServerWorld.getTracking(entity: Entity): Set<ServerPlayerEntity> =
    //$$         getTracker(entity).trackingPlayers
    //$$
    //$$ override fun ServerWorld.updateTrackingState(entity: Entity) {
    //$$     val tracker = getTracker(entity)
    //$$     tracker.pos = SectionPos.from(entity)
    //$$     tracker.entry.tick()
    //$$ }
    //$$
    //$$ var rayTraceContextOverwrite: RayTraceContext? by ThreadLocal()
    //$$ override fun RayTraceContext.withImpl(start: Vec3d, end: Vec3d): RayTraceContext {
    //$$     rayTraceContextOverwrite = this
    //$$     try {
    //$$         @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS") // Magic values for mixin
    //$$         return RayTraceContext(start, end, null, null, null)
    //$$     } finally {
    //$$         rayTraceContextOverwrite = null
    //$$     }
    //$$ }
    //#endif
}
