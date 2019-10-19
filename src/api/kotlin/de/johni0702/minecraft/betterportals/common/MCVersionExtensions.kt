package de.johni0702.minecraft.betterportals.common

import net.minecraft.client.Minecraft
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.nbt.NBTBase
import net.minecraft.nbt.NBTTagList
import net.minecraft.network.Packet
import net.minecraft.server.MinecraftServer
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.RayTraceResult
import net.minecraft.util.math.Vec3d
import net.minecraft.world.World
import net.minecraft.world.WorldServer
import java.util.concurrent.Executor

//#if MC>=11400
//$$ import de.johni0702.minecraft.betterportals.impl.theImpl
//$$ import net.minecraft.client.entity.player.AbstractClientPlayerEntity
//$$ import net.minecraft.client.world.ClientWorld
//$$ import net.minecraft.world.dimension.DimensionType
//$$ import net.minecraftforge.fml.LogicalSide
//$$ import net.minecraftforge.fml.ModList
//#else
import net.minecraft.entity.EntityList
import net.minecraft.launchwrapper.Launch
import net.minecraftforge.fml.common.Loader
import net.minecraftforge.fml.relauncher.Side
//#endif

//#if MC>=11400
//$$ typealias DimensionId = DimensionType
//$$ fun DimensionId.toIntId(): Int = id
//$$ fun Int.toDimensionId(): DimensionId? = DimensionType.getById(this)
//#else
typealias DimensionId = Int
fun DimensionId.toIntId(): Int = this
fun Int.toDimensionId(): DimensionId? = this
//#endif

//#if MC>=11400
//$$ typealias LogicalSide = LogicalSide
//#else
typealias LogicalSide = Side
//#endif

fun hasClass(name: String): Boolean =
        //#if MC>=11400
        //$$ Thread.currentThread().contextClassLoader.getResourceAsStream(name.replace('.', '/') + ".class").also { it?.close() } != null
        //#else
        Launch.classLoader.getClassBytes(name) != null
        //#endif


val MinecraftServer.executor: Executor get() =
//#if MC>=11400
//$$     this
//#else
    Executor { addScheduledTask(it) }
//#endif

val Minecraft.currentlyOnMainThread: Boolean get() =
        //#if MC>=11400
        //$$ isOnExecutionThread
        //#else
        isCallingFromMinecraftThread
        //#endif

val RayTraceResult.hitType: RayTraceResult.Type get() =
//#if MC>=11400
//$$     type
//#else
    typeOfHit
//#endif

val RayTraceResult.hitPos: Vec3d get() =
// Note: While these two look alike, one's a field, the other a property
//#if MC>=11400
//$$     hitVec
//#else
    hitVec
//#endif

fun World.isObstructed(aabb: AxisAlignedBB) =
//#if MC>=11400
//$$         !areCollisionShapesEmpty(aabb)
//$$
//#else
        getCollisionBoxes(null, aabb).isNotEmpty()
//#endif

fun World.forceAddEntity(entity: Entity) {
    val wasForceSpawn = entity.forceSpawn
    entity.forceSpawn = true
    //#if MC>=11400
    //$$ when (this) {
    //$$     is ServerWorld -> if (entity is ServerPlayerEntity) addNewPlayer(entity) else func_217460_e(entity)
    //$$     is ClientWorld -> if (entity is AbstractClientPlayerEntity) addPlayer(entity.entityId, entity) else addEntity(entity.entityId, entity)
    //$$     else -> throw UnsupportedOperationException()
    //$$ }
    //#else
    spawnEntity(entity)
    //#endif
    entity.forceSpawn = wasForceSpawn
}

fun World.forceRemoveEntity(entity: Entity) {
    //#if MC>=11400
    //$$ when (this) {
    //$$     is ServerWorld -> if (entity is ServerPlayerEntity) removePlayer(entity, true) else removeEntity(entity, true)
    //$$     is ClientWorld -> {
    //$$         val entityId = entity.entityId
    //$$         if (getEntityByID(entityId) == entity) {
    //$$             removeEntityFromWorld(entityId)
    //$$         }
    //$$     }
    //$$     else -> throw UnsupportedOperationException()
    //$$ }
    //#else
    removeEntityDangerously(entity)
    //#endif
}

fun NBTTagList.append(nbt: NBTBase) {
    //#if MC>=11400
    //$$ add(nbt)
    //#else
    appendTag(nbt)
    //#endif
}

fun Entity.newEntity(world: World) =
//#if MC>=11400
//$$         type.create(world)
//#else
        EntityList.newEntity(javaClass, world)
//#endif

fun WorldServer.add(entity: Entity) {
    //#if MC>=11400
    //$$ addEntity(entity)
    //#else
    spawnEntity(entity)
    //#endif
}

fun isModLoaded(id: String) =
//#if MC>=11400
//$$         ModList.get().isLoaded(id)
//#else
        Loader.isModLoaded(id)
//#endif

fun WorldServer.sendToTrackingAndSelf(entity: Entity, packet: Packet<*>) {
    //#if MC>=11400
    //$$ chunkProvider.sendToTrackingAndSelf(entity, packet)
    //#else
    entityTracker.sendToTrackingAndSelf(entity, packet)
    //#endif
}

fun WorldServer.getTracking(entity: Entity): Set<EntityPlayerMP> =
//#if MC>=11400
//$$         with(theImpl) { getTracking(entity) }
//#else
        entityTracker.getTrackingPlayers(entity).filterIsInstanceTo(mutableSetOf())
//#endif

fun WorldServer.updateTrackingState(entity: Entity) =
//#if MC>=11400
//$$         with(theImpl) { updateTrackingState(entity) }
//#else
        entityTracker.entries.find { it.trackedEntity == entity }?.updatePlayerList(playerEntities)
//#endif

fun Entity.forcePartialUnmount() =
//#if MC>=11400
//$$         with(theImpl) { forcePartialUnmount() }
//#else
        run { ridingEntity = null }
//#endif
