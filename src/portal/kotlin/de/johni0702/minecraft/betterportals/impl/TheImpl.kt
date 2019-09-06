package de.johni0702.minecraft.betterportals.impl

import de.johni0702.minecraft.betterportals.common.BetterPortalsAPI
import net.minecraft.entity.Entity
import net.minecraft.world.World

//#if MC>=11400
//$$ import net.minecraft.entity.player.ServerPlayerEntity
//$$ import net.minecraft.util.math.RayTraceContext
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

object TheImpl : Impl {
    override val portalApi: BetterPortalsAPI = BetterPortalsAPIImpl

    override fun World.addEntitiesListener(onEntityAdded: (Entity) -> Unit, onEntityRemoved: (Entity) -> Unit) {
        //#if MC>=11400
        //$$ TODO("1.14")
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

    //#if MC>=11400
    //$$ override fun ServerWorld.getTracking(entity: Entity): Set<ServerPlayerEntity> {
    //$$     TODO("1.14")
    //$$ }
    //$$
    //$$ override fun ServerWorld.updateTrackingState(entity: Entity) {
    //$$     TODO("1.14")
    //$$ }
    //$$
    //$$ override fun Entity.forcePartialUnmount() {
    //$$     TODO("1.14")
    //$$ }
    //$$
    //$$ override fun RayTraceContext.withImpl(start: Vec3d, end: Vec3d): RayTraceContext {
    //$$     TODO("1.14")
    //$$ }
    //#endif
}
