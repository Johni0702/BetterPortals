package de.johni0702.minecraft.betterportals.common

import de.johni0702.minecraft.betterportals.impl.theImpl
import io.github.opencubicchunks.cubicchunks.api.util.CubePos
import io.github.opencubicchunks.cubicchunks.api.world.ICubeProviderServer
import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld
import io.github.opencubicchunks.cubicchunks.core.server.CubeProviderServer
import io.github.opencubicchunks.cubicchunks.core.server.ICubicPlayerList
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.item.EntityMinecart
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.Blocks
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.network.PacketBuffer
import net.minecraft.server.management.PlayerList
import net.minecraft.util.BitArray
import net.minecraft.util.EnumFacing
import net.minecraft.util.ResourceLocation
import net.minecraft.util.Rotation
import net.minecraft.util.math.*
import net.minecraft.world.World
import net.minecraft.world.WorldServer
import net.minecraft.world.chunk.BlockStatePaletteHashMap
import net.minecraft.world.chunk.BlockStatePaletteLinear
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.common.eventhandler.Event
import net.minecraftforge.fml.common.eventhandler.EventBus
import net.minecraftforge.registries.IForgeRegistry
import net.minecraftforge.registries.IForgeRegistryEntry
import net.minecraftforge.registries.ObjectHolderRegistry
import org.lwjgl.util.vector.Quaternion
import java.lang.IllegalArgumentException
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.stream.Stream
import javax.vecmath.*
import kotlin.Comparator
import kotlin.collections.HashMap
import kotlin.math.*
import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty
import org.lwjgl.util.vector.Matrix3f as LwjglMatrix3f
import org.lwjgl.util.vector.Matrix4f as LwjglMatrix4f

//#if MC>=11400
//$$ import de.johni0702.minecraft.betterportals.impl.theImpl
//$$ import net.minecraft.world.chunk.Chunk
//$$ import net.minecraft.world.chunk.ChunkStatus
//$$
//$$ typealias BlockStateContainer = net.minecraft.world.chunk.BlockStateContainer<BlockState>
//#else
import net.minecraft.world.chunk.BlockStateContainer
import net.minecraft.client.entity.EntityOtherPlayerMP
import net.minecraft.client.renderer.Matrix4f as McMatrix4f
//#endif

// Generic
val <T> Optional<T>.orNull: T? get() = orElse(null)
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
operator fun <T> Stream<T>.plus(other: Stream<T>): Stream<T> = Stream.concat(this, other)

// MC
val EMPTY_AABB = AxisAlignedBB(0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
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
fun Set<BlockPos>.toImmutable(): Set<BlockPos> = mapTo(mutableSetOf()) { it.toImmutable() }
fun Vec3i.to3d(): Vec3d = Vec3d(this.x.toDouble(), this.y.toDouble(), this.z.toDouble())
fun Vec3i.to3dMid(): Vec3d = this.to3d() + Vec3d(0.5, 0.5, 0.5)
operator fun BlockPos.plus(other: Vec3i): BlockPos = add(other)
operator fun Vec3i.plus(other: Vec3i): Vec3i = Vec3i(x + other.x, y + other.y, z + other.z)
operator fun Vec3i.minus(other: Vec3i): Vec3i = Vec3i(x - other.x, y - other.y, z - other.z)
operator fun Vec3i.times(n: Int): Vec3i = Vec3i(x * n, y * n, z * n)
operator fun Vec3d.plus(other: Vec3d): Vec3d = add(other)
operator fun Vec3d.minus(other: Vec3d): Vec3d = subtract(other)
operator fun Vec3d.unaryMinus(): Vec3d = times(-1)
operator fun Vec3d.times(n: Int): Vec3d = Vec3d(x * n, y * n, z * n)
operator fun Vec3d.times(d: Double): Vec3d = Vec3d(x * d, y * d, z * d)
fun Vec3i.toCubePos() = Vec3i(x shr 4, y shr 4, z shr 4)
fun Vec3d.withoutY(): Vec3d = Vec3d(x, 0.0, z)
fun Vec3d.abs(): Vec3d = Vec3d(Math.abs(x), Math.abs(y), Math.abs(z))
fun Vec3d.toJavaX() = Vector3d(x, y, z)
fun Vec3d.toJavaXPos() = Vector4d(x, y, z, 1.0)
fun Vec3d.toJavaXVec() = Vector4d(x, y, z, 0.0)
fun Vec3d.toPoint() = Point3d(x, y, z)
fun Vec3d.toBlockPos() = BlockPos(this)
fun Vec3d.approxEquals(other: Vec3d, delta: Double) = abs(x - other.x) <= delta && abs(y - other.y) <= delta && abs(z - other.z) <= delta
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

    val sinP = 2.0 * (w * y - z * x)
    val roll: Double
    val pitch: Double
    val yaw: Double
    if (abs(sinP) >= 0.999999) {
        roll = 0.0
        pitch = sign(sinP) * 90
        yaw = 2.0 * atan2(z, w).degrees
    } else {
        val sinRCosP = 2.0 * (w * x + y * z)
        val cosRCosP = 1.0 - 2.0 * (x * x + y * y)
        roll = atan2(sinRCosP, cosRCosP).degrees

        pitch = asin(sinP).degrees

        val sinYCosP = 2.0 * (w * z + x * y)
        val cosYCosP = 1.0 - 2.0 * (y * y + z * z)
        yaw = atan2(sinYCosP, cosYCosP).degrees
    }

    return Vec3d(pitch, (yaw + 360) % 360 - 180, roll)
}
//#if MC<11400
val McMatrix4f.inverse get() = McMatrix4f.invert(this, null)
//#endif
val LwjglMatrix4f.inverse get() = LwjglMatrix4f.invert(this, null)
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
val Rotation.facing: EnumFacing get() = EnumFacing.getHorizontal(ordinal)
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
/**
 * For an AABB which is a plane (i.e. one of [sizeX], [sizeY] or [sizeZ] must be `== 0.0`), returns which axis
 * is perpendicular to the plane.
 * Returns `null` for non-plane AABBs.
 */
val AxisAlignedBB.planeAxis get() = when {
    sizeX == 0.0 -> EnumFacing.Axis.X
    sizeY == 0.0 -> EnumFacing.Axis.Y
    sizeZ == 0.0 -> EnumFacing.Axis.Z
    else -> null
}
/**
 * Calculates interception point of a line segment given by `start` and `end` with a zero-volume (i.e. plane) AABB.
 *
 * As opposed to [AxisAlignedBB.calculateIntercept], this method works even if the `end` point is very close to
 * (or on) the plane. If `start` is on the plane and `end` is not, `null` will be returned.
 * It is an error to call this method on a non-plane AABB (i.e. one of [sizeX], [sizeY] or [sizeZ] must be `== 0.0`).
 */
fun AxisAlignedBB.calculatePlaneIntercept(start: Vec3d, end: Vec3d): Vec3d? {
    val axis = planeAxis ?: throw IllegalArgumentException("AABB is not a plane")
    val diff = end - start
    val diffAxis = diff[axis]
    if (diffAxis == 0.0 || diffAxis == -0.0) return null
    val delta = (min[axis] - start[axis]) / diffAxis
    if (delta <= 0.0 || delta > 1.0) return null
    val result = start + diff * delta
    return if (intersectsWithExcept(axis, result)) result else null
}
fun AxisAlignedBB.intersectsWithExcept(axis: EnumFacing.Axis, vec: Vec3d): Boolean = when(axis) {
    EnumFacing.Axis.X -> vec.y in minY..maxY && vec.z in minZ..maxZ
    EnumFacing.Axis.Y -> vec.x in minX..maxX && vec.z in minZ..maxZ
    EnumFacing.Axis.Z -> vec.x in minX..maxX && vec.y in minY..maxY
}
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

val Entity.eyeOffset get() = Vec3d(0.0, eyeHeight.toDouble(), 0.0)
val Entity.syncPos get() = when {
    this is EntityMinecart && turnProgress > 0 -> minecartPos
    //#if MC>=11400
    //$$ this is LivingEntity && newPosRotationIncrements > 0 -> interpTarget
    //#else
    this is EntityOtherPlayerMP && otherPlayerMPPosRotationIncrements > 0 -> otherPlayerMPPos
    //#endif
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
//#if MC>=11400
//$$ var LivingEntity.interpTarget
//$$     get() = Vec3d(interpTargetX, interpTargetY, interpTargetZ)
//$$     set(value) = with(value) { interpTargetX = x; interpTargetY = y; interpTargetZ = z }
//#else
var EntityOtherPlayerMP.otherPlayerMPPos
    get() = Vec3d(otherPlayerMPX, otherPlayerMPY, otherPlayerMPZ)
    set(value) = with(value) { otherPlayerMPX = x; otherPlayerMPY = y; otherPlayerMPZ = z }
//#endif
var EntityMinecart.minecartPos
    get() = Vec3d(minecartX, minecartY, minecartZ)
    set(value) = with(value) { minecartX = x; minecartY = y; minecartZ = z }

fun ChunkPos.add(x: Int, z: Int) = ChunkPos(this.x + x, this.z + z)

val WorldServer.server get() = minecraftServer!!

//#if MC<11400
@Deprecated("Not on 1.14", ReplaceWith("this.forceAddEntity(entity)"))
fun World.forceSpawnEntity(entity: Entity) = forceAddEntity(entity)
//#endif

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

fun World.makeBulkBlockCache(): BlockCache = if (haveCubicChunks) {
    makeCCCompatibleBlockCache()
} else {
    makeChunkwiseBlockCacheInternal()
}

fun WorldServer.asyncLoadBulkBlockCache(cache: BlockCache, min: BlockPos, max: BlockPos): CompletableFuture<Void> {
    // Cannot use CubePos because this needs to work without CC as well
    val minCube = Vec3i(min.x shr 4, min.y shr 4, min.z shr 4)
    val maxCube = Vec3i(max.x shr 4, (if (haveCubicChunks) max.y else min.y) shr 4, max.z shr 4)
    val loadedChunks = (minCube.x .. maxCube.x).flatMap { x ->
        (minCube.z .. maxCube.z).flatMap { z ->
            (minCube.y .. maxCube.y).map { y ->
                val completableFuture = CompletableFuture<Unit>()
                loadCube(Vec3i(x, y, z)) {
                    cache[BlockPos(x shl 4, y shl 4, z shl 4)]
                    completableFuture.complete(Unit)
                }
                completableFuture
            }
        }
    }
    return CompletableFuture.allOf(*loadedChunks.toTypedArray())
}

private fun WorldServer.loadCube(cubePos: Vec3i, done: () -> Unit) {
    if (isCubicWorld) {
        // Separate method for class loading purposes
        fun doLoadCube() {
            // TODO requires CC update
            //#if MC<11400
            (chunkProvider as CubeProviderServer).asyncGetCube(cubePos.x, cubePos.y, cubePos.z, ICubeProviderServer.Requirement.GENERATE) {
                done()
            }
            //#endif
        }
        doLoadCube()
    } else {
        //#if MC>=11400
        //$$ // TODO is async loading gone?
        //$$ chunkProvider.getChunk(cubePos.x, cubePos.z, true)
        //$$ done()
        //#else
        chunkProvider.loadChunk(cubePos.x, cubePos.z, done)
        //#endif
    }
}

// Separate method due to ICubicWorld class requirement (i.e. only safe to call when CC is loaded!)
private fun World.makeCCCompatibleBlockCache(): BlockCache = if ((this as ICubicWorld).isCubicWorld) {
    makeCubewiseBlockCache()
} else {
    makeChunkwiseBlockCacheInternal()
}

// Only safe to call when CC is loaded and the world uses CC!
private fun World.makeCubewiseBlockCache(): BlockCache =
        HashMap<CubePos, BlockStateContainer?>().let { cache ->
            Gettable { pos ->
                val cubePos = CubePos.fromBlockCoords(pos)
                val storage = cache.getOrPut(cubePos) {
                    //#if MC>=11400
                    //$$ null // TODO
                    //#else
                    (this as ICubicWorld).cubeCache.getCube(cubePos).storage?.data?.copy()
                    //#endif
                }
                storage?.get(pos.x and 15, pos.y and 15, pos.z and 15)
                        ?: Blocks.AIR.defaultState
            }
        }

private fun World.makeChunkwiseBlockCacheInternal(forceLoad: Boolean = true): BlockCache =
        HashMap<ChunkPos, List<BlockStateContainer?>>().let { cache ->
            Gettable { pos ->
                val chunkPos = ChunkPos(pos)
                val storageLists = cache.getOrPut(chunkPos) {
                    //#if MC>=11400
                    //$$ val chunk = getChunk(chunkPos.x, chunkPos.z, ChunkStatus.FULL, forceLoad)
                    //$$ (chunk as? Chunk)?.sections?.map { it?.data?.copy() } ?: emptyList()
                    //#else
                    if (forceLoad || isChunkGeneratedAt(chunkPos.x, chunkPos.z)) {
                        getChunkFromChunkCoords(chunkPos.x, chunkPos.z).blockStorageArray.map { it?.data?.copy() }
                    } else {
                        emptyList()
                    }
                    //#endif
                }
                storageLists.getOrNull(pos.y shr 4)?.get(pos.x and 15, pos.y and 15, pos.z and 15)
                        ?: Blocks.AIR.defaultState
            }
        }

@Deprecated("incompatible with CubicChunks")
fun World.makeChunkwiseBlockCache(forceLoad: Boolean = true): BlockCache = makeChunkwiseBlockCacheInternal(forceLoad)

fun BlockStateContainer.copy(): BlockStateContainer {
    val copy = BlockStateContainer(
            //#if MC>=11400
            //$$ field_205521_b, registry, deserializer, serializer, defaultState
            //#endif
    )
    copy.bits = bits
    copy.palette = when {
        bits <= 4 -> BlockStatePaletteLinear(
                //#if MC>=11400
                //$$ registry, bits, copy, deserializer
                //#else
                bits, copy
                //#endif
        ).apply {
            val oldPalette = palette as BlockStatePaletteLinear
            System.arraycopy(oldPalette.states, 0, states, 0, states.size)
            arraySize = oldPalette.arraySize
        }
        bits <= 8 -> BlockStatePaletteHashMap(
                //#if MC>=11400
                //$$ registry, bits, copy, deserializer, serializer
                //#else
                bits, copy
                //#endif
        ).apply {
            val org = (palette as BlockStatePaletteHashMap).statePaletteMap
            org.forEach { statePaletteMap.put(it, org.getId(it)) }
        }
        else ->
            //#if MC>=11400
            //$$ field_205521_b
            //#else
            BlockStateContainer.REGISTRY_BASED_PALETTE
            //#endif
    }
    copy.storage = BitArray(bits, 4096)
    System.arraycopy(storage.backingLongArray, 0, copy.storage.backingLongArray, 0, storage.backingLongArray.size)
    return copy
}

class ObjectHolder<in R, T: U, U: IForgeRegistryEntry<U>>(
        private val registry: IForgeRegistry<U>,
        val id: ResourceLocation
) : ReadOnlyProperty<R, T> {
    constructor(registry: IForgeRegistry<U>, id: String)
            : this(registry, ResourceLocation(id))

    private var value: T? = null

    init {
        //#if MC>=11400
        //$$ theImpl.addObjectHolderHandler { filter ->
        //$$     if (!filter(registry.registryName)) {
        //$$         return@addObjectHolderHandler
        //$$     }
        //$$     @Suppress("UNCHECKED_CAST")
        //$$     value = registry.getValue(id) as T?
        //$$ }
        //#else
        theImpl.addObjectHolderHandler { _ ->
            @Suppress("UNCHECKED_CAST")
            value = registry.getValue(id) as T?
        }
        //#endif
    }

    override fun getValue(thisRef: R, property: KProperty<*>): T = value!!
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
    //#if MC>=11400
    //$$ with(from) { matrix * motion.toJavaX() }.let { pos ->
    //$$     to.motion = pos.toMC()
    //$$ }
    //#else
    with(from) { matrix * Vector3d(motionX, motionY, motionZ) }.let { pos ->
        to.motionX = pos.x
        to.motionY = pos.y
        to.motionZ = pos.z
    }
    //#endif

    to.rotationYaw = from.rotationYaw + yawOffset
    to.prevRotationYaw = from.prevRotationYaw + yawOffset
    to.rotationPitch = from.rotationPitch
    to.prevRotationPitch = from.prevRotationPitch

    if (to is EntityPlayer && from is EntityPlayer) {
        to.cameraYaw = from.cameraYaw
        to.prevCameraYaw = from.prevCameraYaw
        //#if MC<11400
        to.cameraPitch = from.cameraPitch
        to.prevCameraPitch = from.prevCameraPitch

        // Sneaking
        to.height = from.height
        //#endif
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

fun World.findPortal(start: Vec3d, end: Vec3d): Triple<World, Vec3d, PortalAgent<*>?> {
    return portalManager.loadedPortals.filter { agent ->
        val portal = agent.portal
        val vec = portal.localFacing.directionVec.to3d() * 0.5
        val negVec = vec * -1
        agent.portal.localBoundingBox.contract(vec).contract(negVec).calculatePlaneIntercept(start, end) != null
    }.flatMap { agent ->
        val portal = agent.portal
        val vec = portal.localFacing.directionVec.to3d() * 0.5
        val negVec = vec * -1
        portal.localDetailedBounds.mapNotNull {
            it.contract(vec).contract(negVec).calculatePlaneIntercept(start, end)?.let { hitVec ->
                Triple(agent.remoteWorldIfLoaded ?: return@let null, hitVec, agent)
            }
        }
    }.minBy {
        it.second.squareDistanceTo(start)
    } ?: Triple(this, end, null)
}

fun World.rayTracePortals(start: Vec3d, end: Vec3d): Pair<World, Matrix4d> {
    fun aux(world: World, start: Vec3d, end: Vec3d, mat: Matrix4d): Pair<World, Matrix4d> {
        val result = world.findPortal(start, end)
        val agent = result.third ?: return Pair(world, mat)
        val newStart = (agent.portal.localToRemoteMatrix * result.second.toPoint()).toMC()
        val newEnd = (agent.portal.localToRemoteMatrix * end.toPoint()).toMC()
        return aux(result.first, newStart, newEnd, mat * agent.portal.localToRemoteMatrix)
    }
    return aux(this, start, end, Mat4d.id())
}

//#if MC>=11400
//$$ val RayTraceContext.start get() = func_222253_b()
//$$ val RayTraceContext.end get() = func_222250_a()
//$$ operator fun RayTraceContext.component1(): Vec3d = start
//$$ operator fun RayTraceContext.component2(): Vec3d = end
//$$ fun RayTraceContext.with(start: Vec3d = this.start, end: Vec3d = this.end) = with(theImpl) { withImpl(start, end) }
//#endif

fun World.rayTraceBlocksWithPortals(
        //#if MC>=11400
        //$$ context: RayTraceContext
        //#else
        start: Vec3d,
        end: Vec3d,
        stopOnLiquid: Boolean = false,
        ignoreBlockWithoutBoundingBox: Boolean = false,
        returnLastUncollidableBlock: Boolean = false
        //#endif
): RayTraceResult? {
    //#if MC>=11400
    //$$ val (start, end) = context
    //#endif
    val result = findPortal(start, end)
    val agent = result.third
            //#if MC>=11400
            //$$ ?: return rayTraceBlocks(context)
            //#else
            ?: return rayTraceBlocks(start, end, stopOnLiquid, ignoreBlockWithoutBoundingBox, returnLastUncollidableBlock)
            //#endif
    val hitVec = result.second

    val localResult = rayTraceBlocks(
            //#if MC>=11400
            //$$ context.with(end = hitVec)
            //#else
            start,
            hitVec,
            stopOnLiquid,
            ignoreBlockWithoutBoundingBox,
            returnLastUncollidableBlock
            //#endif
    )
    val remoteWorld = agent.remoteWorldIfLoaded ?: return localResult
    localResult?.let {
        if (it.hitType != RayTraceResult.Type.MISS) {
            return it
        }
    }

    val portal = agent.portal
    val remotePortalExit = with(portal) {
        hitVec.fromLocal().toRemote()
    }
    val remoteEnd = with(portal) {
        end.fromLocal().toRemote()
    }
    val remoteResult = remoteWorld.rayTraceBlocksWithPortals(
            //#if MC>=11400
            //$$ context.with(start = remotePortalExit, end = remoteEnd)
            //#else
            remotePortalExit,
            remoteEnd,
            stopOnLiquid,
            ignoreBlockWithoutBoundingBox,
            returnLastUncollidableBlock
            //#endif
    ) ?: return null
    val localHitVec = with(portal) { remoteResult.hitPos.fromRemote().toLocal() }
    //#if MC>=11400
    //$$ return when (remoteResult) {
    //$$     is EntityRayTraceResult -> EntityRayTraceResult(remoteResult.entity, localHitVec)
    //$$     is BlockRayTraceResult -> BlockRayTraceResult(
    //$$             localHitVec,
    //$$             (portal.localRotation - portal.remoteRotation).rotate(remoteResult.face),
    //$$             with(portal) { remoteResult.pos.fromRemote().toLocal() },
    //$$             remoteResult.isInside
    //$$     )
    //$$     else -> null
    //$$ }
    //#else
    return if (remoteResult.hitType == RayTraceResult.Type.ENTITY) {
        RayTraceResult(remoteResult.entityHit, localHitVec)
    } else {
        RayTraceResult(
                remoteResult.hitType,
                localHitVec,
                (portal.localRotation - portal.remoteRotation).rotate(remoteResult.sideHit),
                with(portal) { remoteResult.blockPos.fromRemote().toLocal() }
        )
    }
    //#endif
}

val haveCubicChunks by lazy { isModLoaded("cubicchunks") }

val World.isCubicWorld get() = haveCubicChunks && isCubicWorldUnsafe
// Only safe to call when CC is loaded!
private val World.isCubicWorldUnsafe get() = (this as ICubicWorld).isCubicWorld

val PlayerList.verticalViewDistance get() = if (haveCubicChunks) verticalViewDistanceUnsafe else viewDistance
// Only safe to call when CC is loaded!
private val PlayerList.verticalViewDistanceUnsafe get() = (this as ICubicPlayerList).verticalViewDistance

operator fun <T, V> ThreadLocal<V>.provideDelegate(thisRef: T, prop: KProperty<*>): ReadWriteProperty<T, V?> = object : ReadWriteProperty<T, V?> {
    override fun getValue(thisRef: T, property: KProperty<*>): V? = get()

    override fun setValue(thisRef: T, property: KProperty<*>, value: V?) {
        if (value != null) {
            set(value)
        } else {
            remove()
        }
    }
}

val hasVivecraft by lazy { hasClass("org.vivecraft.asm.VivecraftASMTransformer") }
