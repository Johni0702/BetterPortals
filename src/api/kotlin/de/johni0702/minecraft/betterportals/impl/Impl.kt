package de.johni0702.minecraft.betterportals.impl

import de.johni0702.minecraft.betterportals.common.BetterPortalsAPI
import net.minecraft.entity.Entity
import net.minecraft.util.ResourceLocation
import net.minecraft.world.World

//#if FABRIC>=1
//$$ import net.minecraft.network.Packet
//#endif

//#if MC>=11400
//$$ import net.minecraft.client.Minecraft
//$$ import net.minecraft.entity.player.ServerPlayerEntity
//$$ import net.minecraft.util.math.RayTraceContext
//$$ import net.minecraft.util.math.Vec3d
//$$ import net.minecraft.world.server.ServerWorld
//$$ import java.util.*
//$$ import java.util.concurrent.BlockingQueue
//#endif

lateinit var theImpl: Impl

interface Impl {
    val portalApi: BetterPortalsAPI

    fun World.addEntitiesListener(onEntityAdded: (Entity) -> Unit, onEntityRemoved: (Entity) -> Unit)
    fun addObjectHolderHandler(handler: (filter: (ResourceLocation) -> Boolean) -> Unit)

    //#if MC>=11400
    //$$ fun ServerWorld.getTracking(entity: Entity): Set<ServerPlayerEntity>
    //$$ fun ServerWorld.updateTrackingState(entity: Entity)
    //$$ fun RayTraceContext.withImpl(start: Vec3d, end: Vec3d): RayTraceContext
    //#endif

    //#if FABRIC>=1
    //$$ // Until https://github.com/FabricMC/fabric/pull/384 or something similar is available
    //$$ fun createSpawnPacket(entity: Entity): Packet<*>
    //#endif
}

//#if MC>=11400
//$$ interface IThreadTaskExecutor<R: Runnable> {
//$$     val queue: Queue<R>
//$$     var blockingQueue: BlockingQueue<R>?
//$$
//$$     companion object {
//$$         @Suppress("UNCHECKED_CAST")
//$$         fun from(mc: Minecraft): IThreadTaskExecutor<Runnable> = (mc as IThreadTaskExecutor<Runnable>)
//$$     }
//$$ }
//#endif
