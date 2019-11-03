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
//#if FABRIC>=1
//$$ import net.fabricmc.loader.api.FabricLoader
//$$ import net.minecraft.world.dimension.TheNetherDimension
//#else
//$$ import net.minecraftforge.fml.LogicalSide
//$$ import net.minecraftforge.fml.ModList
//#endif
//#else
import de.johni0702.minecraft.betterportals.impl.accessors.AccEntityTracker
import net.minecraft.entity.EntityList
import net.minecraft.launchwrapper.Launch
import net.minecraftforge.fml.common.Loader
import net.minecraftforge.fml.relauncher.Side
//#endif

//#if MC>=11400
//$$ typealias DimensionId = DimensionType
//$$ fun DimensionId.toIntId(): Int = id
//$$ fun Int.toDimensionId(): DimensionId? = DimensionType.getById(this)
//$$ val World.dimensionId: DimensionId get() = dimension.type
//#else
typealias DimensionId = Int
fun DimensionId.toIntId(): Int = this
fun Int.toDimensionId(): DimensionId? = this
val World.dimensionId: DimensionId get() = provider.dimension
//#endif

//#if FABRIC>=1
//$$ enum class LogicalSide {
//$$     CLIENT, SERVER
//$$ }
//#else
//#if MC>=11400
//$$ typealias LogicalSide = LogicalSide
//#else
typealias LogicalSide = Side
//#endif
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
        // TODO why does the preprocessor not remap this?
        //#if FABRIC>=1
        //$$ isOnThread
        //#else
        //#if MC>=11400
        //$$ isOnExecutionThread
        //#else
        isCallingFromMinecraftThread
        //#endif
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
        //#if FABRIC>=1
        //$$ is ServerWorld -> if (entity is ServerPlayerEntity) removePlayer(entity) else removeEntity(entity)
        //#else
        //$$ is ServerWorld -> if (entity is ServerPlayerEntity) removePlayer(entity, true) else removeEntity(entity, true)
        //#endif
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
//#if FABRIC>=1
//$$         FabricLoader.getInstance().isModLoaded(id)
//#else
//#if MC>=11400
//$$         ModList.get().isLoaded(id)
//#else
        Loader.isModLoaded(id)
//#endif
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
        (entityTracker as AccEntityTracker).entries.find { it.trackedEntity == entity }?.updatePlayerList(playerEntities)
//#endif

//#if FABRIC>=1
//$$ val World.theActualHeight: Int get() = if (dimension is TheNetherDimension) 128 else 256
//$$ val World.theMovementFactor: Double get() = if (dimension is TheNetherDimension) 8.0 else 0.0
//#else
val World.theActualHeight: Int get() = provider.actualHeight
val World.theMovementFactor: Double get() = provider.movementFactor
//#endif
