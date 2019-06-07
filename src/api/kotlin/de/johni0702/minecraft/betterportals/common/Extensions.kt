package de.johni0702.minecraft.betterportals.common

import net.minecraft.block.state.IBlockState
import net.minecraft.client.entity.EntityOtherPlayerMP
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.EntityTracker
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.init.Blocks
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.network.PacketBuffer
import net.minecraft.util.BitArray
import net.minecraft.util.EnumFacing
import net.minecraft.util.Rotation
import net.minecraft.util.math.*
import net.minecraft.world.World
import net.minecraft.world.WorldServer
import net.minecraft.world.chunk.BlockStateContainer
import net.minecraft.world.chunk.BlockStatePaletteHashMap
import net.minecraft.world.chunk.BlockStatePaletteLinear
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.common.eventhandler.Event
import net.minecraftforge.fml.common.eventhandler.EventBus
import org.lwjgl.util.vector.Quaternion
import javax.vecmath.*
import kotlin.math.*
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty
import net.minecraft.client.renderer.Matrix4f as McMatrix4f
import org.lwjgl.util.vector.Matrix3f as LwjglMatrix3f
import org.lwjgl.util.vector.Vector4f as LwjglVector4f

// Generic
fun <T> MutableList<T>.removeAtOrNull(index: Int) = if (isEmpty()) null else removeAt(index)
fun <T> MutableList<T>.popOrNull() = removeAtOrNull(0)
fun <T> MutableList<T>.takeLast() = removeAt(lastIndex)
fun Matrix4d.inverse() = Mat4d.inverse(this)
operator fun Matrix4d.times(other: Matrix4d) = Matrix4d().also { it.mul(this, other) }
operator fun Matrix4d.times(other: Vector3d) = Vector3d().also { transform(other, it) }
operator fun Matrix4d.times(other: Point3d) = Point3d().also { transform(other, it) }
inline operator fun <reified T : Tuple4d> Matrix4d.times(other: T) = (other.clone() as T).also { transform(other, it) }
object Mat4d {
    @JvmStatic
    fun id() = Matrix4d().apply { setIdentity() }
    @JvmStatic
    fun add(dx: Double, dy: Double, dz: Double) = add(Vector3d(dx, dy, dz))
    @JvmStatic
    fun add(vec: Vector3d) = id().apply { setTranslation(vec) }
    @JvmStatic
    fun sub(dx: Double, dy: Double, dz: Double) = sub(Vector3d(dx, dy, dz))
    @JvmStatic
    fun sub(vec: Vector3d) = id().apply { setTranslation(Vector3d().also { it.negate(vec) }) }
    @JvmStatic
    fun rotYaw(angle: Number) = id().apply { setRotation(AxisAngle4d(0.0, -1.0, 0.0, Math.toRadians(angle.toDouble()))) }
    @JvmStatic
    fun inverse(of: Matrix4d) = id().apply { invert(of) }
}

// MC
val Float.radians get() = toDouble().radians.toFloat()
val Float.degrees get() = toDouble().degrees.toFloat()
val Double.radians get() = Math.toRadians(this)
val Double.degrees get() = Math.toDegrees(this)
val EnumFacing.Plane.axes get() = EnumFacing.Axis.values().filter { it.plane == this }
val EnumFacing.Plane.perpendicularAxes get() = opposite.axes
val EnumFacing.Plane.opposite get() = when(this) {
    EnumFacing.Plane.HORIZONTAL -> EnumFacing.Plane.VERTICAL
    EnumFacing.Plane.VERTICAL -> EnumFacing.Plane.HORIZONTAL
}
fun Rotation.axis(plane: EnumFacing.Plane): EnumFacing.Axis = when(plane) {
    EnumFacing.Plane.HORIZONTAL -> this.facing.axis
    EnumFacing.Plane.VERTICAL -> EnumFacing.Axis.Y
}
val EnumFacing.Axis.perpendicularPlane get() = plane.opposite
fun EnumFacing.Axis.toFacing(direction: EnumFacing.AxisDirection): EnumFacing = EnumFacing.getFacingFromAxis(direction, this)
fun EnumFacing.Axis.toFacing(direction: Double)
        = toFacing(if (direction > 0) EnumFacing.AxisDirection.POSITIVE else EnumFacing.AxisDirection.NEGATIVE)
fun EnumFacing.Axis.toFacing(direction: Vec3d) = toFacing(direction[this])
val EnumFacing.Axis.parallelFaces get() = EnumFacing.values().filter { it.axis != this }
fun Vec3i.to3d(): Vec3d = Vec3d(this.x.toDouble(), this.y.toDouble(), this.z.toDouble())
fun Vec3i.to3dMid(): Vec3d = this.to3d() + Vec3d(0.5, 0.5, 0.5)
operator fun Vec3i.plus(other: Vec3i): Vec3i = Vec3i(x + other.x, y + other.y, z + other.z)
operator fun Vec3i.times(n: Int): Vec3i = Vec3i(x * n, y * n, z * n)
operator fun Vec3d.plus(other: Vec3d): Vec3d = add(other)
operator fun Vec3d.minus(other: Vec3d): Vec3d = subtract(other)
operator fun Vec3d.times(n: Int): Vec3d = Vec3d(x * n, y * n, z * n)
operator fun Vec3d.times(d: Double): Vec3d = Vec3d(x * d, y * d, z * d)
fun Vec3d.withoutY(): Vec3d = Vec3d(x, 0.0, y)
fun Vec3d.abs(): Vec3d = Vec3d(Math.abs(x), Math.abs(y), Math.abs(z))
fun Vec3d.toJavaX() = Vector3d(x, y, z)
fun Vec3d.toJavaXPos() = Vector4d(x, y, z, 1.0)
fun Vec3d.toJavaXVec() = Vector4d(x, y, z, 0.0)
fun Vec3d.toPoint() = Point3d(x, y, z)
fun Vec3d.toBlockPos() = BlockPos(this)
fun Vector3d.toMC() = Vec3d(x, y, z)
fun Vector4d.toMC() = Vec3d(x, y, z)
fun Point3d.toMC() = Vec3d(x, y, z)
fun Vec3d.toQuaternion(): Quaternion {
    val pitch = x.radians
    val yaw = (y + 180).radians
    val roll = z.radians

    val cy = cos(yaw * 0.5)
    val sy = sin(yaw * 0.5)
    val cp = cos(pitch * 0.5)
    val sp = sin(pitch * 0.5)
    val cr = cos(roll * 0.5)
    val sr = sin(roll * 0.5)

    val w = (cy * cp * cr + sy * sp * sr).toFloat()
    val x = (cy * cp * sr - sy * sp * cr).toFloat()
    val y = (sy * cp * sr + cy * sp * cr).toFloat()
    val z = (sy * cp * cr - cy * sp * sr).toFloat()
    return Quaternion(-y, -z, x, w)
}
operator fun Quaternion.times(other: Quaternion): Quaternion = Quaternion.mul(this, other, null)
fun Quaternion.toPitchYawRoll(): Vec3d {
    val x = this.z
    val y = -this.x
    val z = -this.y

    val sinRCosP = 2.0 * (w * x + y * z)
    val cosRCosP = 1.0 - 2.0 * (x * x + y * y)
    val roll = atan2(sinRCosP, cosRCosP).degrees

    val sinP = 2.0 * (w * y - z * x)
    val pitch = if (abs(sinP) >= 1) sign(sinP) * 90 else asin(sinP).degrees

    // yaw (z-axis rotation)
    val sinYCosP = 2.0 * (w * z + x * y)
    val cosYCosP = 1.0 - 2.0 * (y * y + z * z)
    val yaw = atan2(sinYCosP, cosYCosP).degrees

    return Vec3d(pitch, (yaw + 360) % 360 - 180, roll)
}
val McMatrix4f.inverse get() = McMatrix4f.invert(this, null)
fun Matrix4d.toJX4f() = Matrix4f(this)
fun Matrix4f.toLwjgl3f() = LwjglMatrix3f().also {
    it.m00 = m00
    it.m01 = m01
    it.m02 = m02
    it.m10 = m10
    it.m11 = m11
    it.m12 = m12
    it.m20 = m20
    it.m21 = m21
    it.m22 = m22
}
fun LwjglMatrix3f.extractRotation(): Quaternion = Quaternion.setFromMatrix(this, Quaternion())
operator fun Vec3d.get(axis: EnumFacing.Axis) = when(axis) {
    EnumFacing.Axis.X -> x
    EnumFacing.Axis.Y -> y
    EnumFacing.Axis.Z -> z
}
fun Vec3d.with(axis: EnumFacing.Axis, value: Double) = when(axis) {
    EnumFacing.Axis.X -> Vec3d(value, y, z)
    EnumFacing.Axis.Y -> Vec3d(x, value, z)
    EnumFacing.Axis.Z -> Vec3d(x, y, value)
}
fun Vec3d.rotate(rot: Rotation): Vec3d = when(rot) {
    Rotation.NONE -> this
    Rotation.CLOCKWISE_90 -> Vec3d(-z, y, x)
    Rotation.CLOCKWISE_180 -> Vec3d(-x, y, -z)
    Rotation.COUNTERCLOCKWISE_90 -> Vec3d(z, y, -x)
}
fun EnumFacing.toRotation(): Rotation = if (horizontalIndex == -1) Rotation.NONE else Rotation.values()[horizontalIndex]
val Rotation.facing: EnumFacing get() = EnumFacing.HORIZONTALS[ordinal]
val Rotation.degrees get() = ordinal * 90
val Rotation.reverse get() = when(this) {
    Rotation.CLOCKWISE_90 -> Rotation.COUNTERCLOCKWISE_90
    Rotation.COUNTERCLOCKWISE_90 -> Rotation.CLOCKWISE_90
    else -> this
}
operator fun Rotation.plus(other: Rotation): Rotation = add(other)
operator fun Rotation.minus(other: Rotation): Rotation = add(other.reverse)
val AxisAlignedBB_INFINITE = AxisAlignedBB(
        Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY,
        Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY
)
fun AxisAlignedBB.with(facing: EnumFacing, value: Double) = when(facing) {
    EnumFacing.DOWN -> AxisAlignedBB(minX, value, minZ, maxX, maxY, maxZ)
    EnumFacing.UP -> AxisAlignedBB(minX, minY, minZ, maxX, value, maxZ)
    EnumFacing.NORTH -> AxisAlignedBB(minX, minY, value, maxX, maxY, maxZ)
    EnumFacing.SOUTH -> AxisAlignedBB(minX, minY, minZ, maxX, maxY, value)
    EnumFacing.WEST -> AxisAlignedBB(value, minY, minZ, maxX, maxY, maxZ)
    EnumFacing.EAST -> AxisAlignedBB(minX, minY, minZ, value, maxY, maxZ)
}
fun AxisAlignedBB.grow(by: Vec3d): AxisAlignedBB = grow(by.x, by.y, by.z)
fun AxisAlignedBB.expand(by: Vec3d): AxisAlignedBB = expand(by.x, by.y, by.z)
fun AxisAlignedBB.contract(by: Vec3d): AxisAlignedBB = contract(by.x, by.y, by.z)
val AxisAlignedBB.sizeX get() = maxX - minX
val AxisAlignedBB.sizeY get() = maxY - minY
val AxisAlignedBB.sizeZ get() = maxZ - minZ
val AxisAlignedBB.maxSideLength get() = max(sizeX, max(sizeY, sizeZ))
val AxisAlignedBB.min get() = Vec3d(minX, minY, minZ)
val AxisAlignedBB.max get() = Vec3d(maxX, maxY, maxZ)
// Note: the obvious choice of constructor is @SideOnly(Client)
fun Vec3d.toAxisAlignedBB(other: Vec3d) = AxisAlignedBB(x, y, z, other.x, other.y, other.z)
fun Collection<BlockPos>.toAxisAlignedBB(): AxisAlignedBB =
        if (isEmpty()) AxisAlignedBB(BlockPos.ORIGIN, BlockPos.ORIGIN)
        else map(::AxisAlignedBB).reduce(AxisAlignedBB::union)
fun Collection<BlockPos>.minByAnyCoord() =
        minWith(Comparator.comparingInt<BlockPos> { it.x }.thenComparingInt { it.y }.thenComparingInt { it.z })

fun NBTTagCompound.setXYZ(pos: BlockPos): NBTTagCompound {
    setInteger("x", pos.x)
    setInteger("y", pos.y)
    setInteger("z", pos.z)
    return this
}
fun NBTTagCompound.getXYZ(): BlockPos = BlockPos(getInteger("x"), getInteger("y"), getInteger("z"))

inline fun <reified T : Enum<T>> PacketBuffer.readEnum(): T = readEnumValue(T::class.java)
inline fun <reified T : Enum<T>> PacketBuffer.writeEnum(value: T): PacketBuffer = writeEnumValue(value)

@Suppress("UNCHECKED_CAST") // Why forge? why?
fun EntityTracker.getTracking(entity: Entity): Set<EntityPlayerMP> = getTrackingPlayers(entity) as Set<EntityPlayerMP>

val Entity.eyeOffset get() = Vec3d(0.0, eyeHeight.toDouble(), 0.0)
val Entity.syncPos get() = when {
    this is EntityOtherPlayerMP && otherPlayerMPPosRotationIncrements > 0 -> otherPlayerMPPos
    else -> pos
}
var Entity.pos
    get() = Vec3d(posX, posY, posZ)
    set(value) = with(value) { posX = x; posY = y; posZ = z }
var Entity.lastTickPos
    get() = Vec3d(lastTickPosX, lastTickPosY, lastTickPosZ)
    set(value) = with(value) { lastTickPosX = x; lastTickPosY = y; lastTickPosZ = z }
var Entity.prevPos
    get() = Vec3d(prevPosX, prevPosY, prevPosZ)
    set(value) = with(value) { prevPosX = x; prevPosY = y; prevPosZ = z }
var EntityOtherPlayerMP.otherPlayerMPPos
    get() = Vec3d(otherPlayerMPX, otherPlayerMPY, otherPlayerMPZ)
    set(value) = with(value) { otherPlayerMPX = x; otherPlayerMPY = y; otherPlayerMPZ = z }

fun ChunkPos.add(x: Int, z: Int) = ChunkPos(this.x + x, this.z + z)

val WorldServer.server get() = minecraftServer!!

fun World.forceSpawnEntity(entity: Entity) {
    val wasForceSpawn = entity.forceSpawn
    entity.forceSpawn = true
    spawnEntity(entity)
    entity.forceSpawn = wasForceSpawn
}

class Gettable<in K, out V>(
        private val getter: (K) -> V
) {
    operator fun get(key: K): V = getter(key)
    operator fun invoke(key: K) = getter(key)
}
typealias BlockCache = Gettable<BlockPos, IBlockState>

fun World.makeBlockCache(forceLoad: Boolean = true): BlockCache =
        HashMap<BlockPos, IBlockState>().let { cache ->
            Gettable { pos ->
                cache.getOrPut(pos) {
                    if (forceLoad || isBlockLoaded(pos)) getBlockState(pos) else Blocks.AIR.defaultState
                }
            }
        }

fun World.makeChunkwiseBlockCache(forceLoad: Boolean = true): BlockCache =
        HashMap<ChunkPos, List<BlockStateContainer?>>().let { cache ->
            Gettable { pos ->
                val chunkPos = ChunkPos(pos)
                val storageLists = cache.getOrPut(chunkPos) {
                    if (forceLoad || isChunkGeneratedAt(chunkPos.x, chunkPos.z)) {
                        getChunkFromChunkCoords(chunkPos.x, chunkPos.z).blockStorageArray.map { it?.data?.copy() }
                    } else {
                        emptyList()
                    }
                }
                storageLists.getOrNull(pos.y shr 4)?.get(pos.x and 15, pos.y and 15, pos.z and 15)
                        ?: Blocks.AIR.defaultState
            }
        }

fun BlockStateContainer.copy(): BlockStateContainer {
    val copy = BlockStateContainer()
    copy.bits = bits
    copy.palette = when {
        bits <= 4 -> BlockStatePaletteLinear(bits, copy).apply {
            val oldPalette = palette as BlockStatePaletteLinear
            System.arraycopy(oldPalette.states, 0, states, 0, states.size)
            arraySize = oldPalette.arraySize
        }
        bits <= 8 -> BlockStatePaletteHashMap(bits, copy).apply {
            val org = (palette as BlockStatePaletteHashMap).statePaletteMap
            org.forEach { statePaletteMap.put(it, org.getId(it)) }
        }
        else -> BlockStateContainer.REGISTRY_BASED_PALETTE
    }
    copy.storage = BitArray(bits, 4096)
    System.arraycopy(storage.backingLongArray, 0, copy.storage.backingLongArray, 0, storage.backingLongArray.size)
    return copy
}

operator fun <T> EventBus.provideDelegate(thisRef: T, prop: KProperty<*>): ReadWriteProperty<T, Boolean>
        = EventBusRegistration(this)

private class EventBusRegistration<in T>(
        val eventBus: EventBus
) : ReadWriteProperty<T, Boolean> {
    var registered = false

    override fun getValue(thisRef: T, property: KProperty<*>): Boolean = registered

    override fun setValue(thisRef: T, property: KProperty<*>, value: Boolean) {
        if (value) eventBus.register(thisRef)
        else eventBus.unregister(thisRef)
        this.registered = value
    }

}

fun <T: Event> T.post() = apply { MinecraftForge.EVENT_BUS.post(this) }


fun Entity.derivePosRotFrom(from: Entity, portal: Portal) {
    val rotation = portal.remoteRotation - portal.localRotation
    derivePosRotFrom(from, portal.localToRemoteMatrix, rotation.degrees.toFloat())
}

fun Entity.derivePosRotFrom(from: Entity, matrix: Matrix4d, yawOffset: Float) {
    val to = this
    with(from) { matrix * Point3d(posX, posY, posZ) }.let { pos ->
        to.setPosition(pos.x, pos.y, pos.z)
    }
    with(from) { matrix * Point3d(prevPosX, prevPosY, prevPosZ) }.let { pos ->
        to.prevPosX = pos.x
        to.prevPosY = pos.y
        to.prevPosZ = pos.z
    }
    with(from) { matrix * Point3d(lastTickPosX, lastTickPosY, lastTickPosZ) }.let { pos ->
        to.lastTickPosX = pos.x
        to.lastTickPosY = pos.y
        to.lastTickPosZ = pos.z
    }
    with(from) { matrix * Vector3d(motionX, motionY, motionZ) }.let { pos ->
        to.motionX = pos.x
        to.motionY = pos.y
        to.motionZ = pos.z
    }

    to.rotationYaw = from.rotationYaw + yawOffset
    to.prevRotationYaw = from.prevRotationYaw + yawOffset
    to.rotationPitch = from.rotationPitch
    to.prevRotationPitch = from.prevRotationPitch

    if (to is EntityPlayer && from is EntityPlayer) {
        to.cameraYaw = from.cameraYaw
        to.prevCameraYaw = from.prevCameraYaw
        to.cameraPitch = from.cameraPitch
        to.prevCameraPitch = from.prevCameraPitch

        // Sneaking
        to.height = from.height
    }

    if (to is EntityLivingBase && from is EntityLivingBase) {
        to.limbSwing = from.limbSwing
        to.limbSwingAmount = from.limbSwingAmount
        to.prevLimbSwingAmount = from.prevLimbSwingAmount

        to.rotationYawHead = from.rotationYawHead + yawOffset
        to.prevRotationYawHead = from.prevRotationYawHead + yawOffset
        to.renderYawOffset = from.renderYawOffset + yawOffset
        to.prevRenderYawOffset = from.prevRenderYawOffset + yawOffset
    }

    to.distanceWalkedModified = from.distanceWalkedModified
    to.prevDistanceWalkedModified = from.prevDistanceWalkedModified
    to.isSneaking = from.isSneaking
    to.isSprinting = from.isSprinting
}

fun World.rayTraceBlocksWithPortals(
        start: Vec3d,
        end: Vec3d,
        stopOnLiquid: Boolean = false,
        ignoreBlockWithoutBoundingBox: Boolean = false,
        returnLastUncollidableBlock: Boolean = false
): RayTraceResult? {
    val result = portalManager.loadedPortals.filter { agent ->
        agent.portal.localBoundingBox.calculateIntercept(start, end) != null
    }.flatMap { agent ->
        val portal = agent.portal
        portal.localDetailedBounds.mapNotNull {
            it.calculateIntercept(start, end)?.let { entry ->
                it.calculateIntercept(end, start)?.let { exit ->
                    val entrySide = portal.localAxis.toFacing(portal.localPosition.to3dMid() - entry.hitVec)
                    val exitSide = portal.localAxis.toFacing(portal.localPosition.to3dMid() - exit.hitVec)
                    if (entrySide != exitSide) {
                        Triple(agent, entry.hitVec, exit.hitVec)
                    } else {
                        null
                    }
                }
            }
        }
    }.minBy {
        it.second.squareDistanceTo(start)
    } ?: return rayTraceBlocks(start, end, stopOnLiquid, ignoreBlockWithoutBoundingBox, returnLastUncollidableBlock)

    val agent = result.first
    val portalEntry = result.second
    val portalExit = result.third

    val localResult = rayTraceBlocks(
            start,
            portalEntry,
            stopOnLiquid,
            ignoreBlockWithoutBoundingBox,
            returnLastUncollidableBlock
    )
    val remoteWorld = agent.view?.camera?.world ?: return localResult
    localResult?.let {
        if (it.typeOfHit != RayTraceResult.Type.MISS) {
            return it
        }
    }

    val portal = agent.portal
    val remotePortalExit = with(portal) {
        portalExit.fromLocal().toRemote()
    }
    val remoteEnd = with(portal) {
        end.fromLocal().toRemote()
    }
    val remoteResult = remoteWorld.rayTraceBlocksWithPortals(
            remotePortalExit,
            remoteEnd,
            stopOnLiquid,
            ignoreBlockWithoutBoundingBox,
            returnLastUncollidableBlock
    ) ?: return null
    val localHitVec = with(portal) { remoteResult.hitVec.fromRemote().toLocal() }
    return if (remoteResult.typeOfHit == RayTraceResult.Type.ENTITY) {
        RayTraceResult(remoteResult.entityHit, localHitVec)
    } else {
        RayTraceResult(
                remoteResult.typeOfHit,
                localHitVec,
                (portal.localRotation - portal.remoteRotation).rotate(remoteResult.sideHit),
                with(portal) { remoteResult.blockPos.fromRemote().toLocal() }
        )
    }
}
