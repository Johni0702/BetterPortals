package de.johni0702.minecraft.view.server

import de.johni0702.minecraft.betterportals.common.*
import net.minecraft.util.math.ChunkPos
import net.minecraft.util.math.Vec3d
import net.minecraft.util.math.Vec3i
import net.minecraft.world.WorldServer
import kotlin.math.absoluteValue
import kotlin.math.max

/**
 * Represents a specific part/view of one world which will be synced to the client.
 *
 * Multiple views on the same or different worlds may exist and overlapping views will share underlying data (e.g.
 * chunks, entities, etc.). As such, a new view can be created at almost zero cost if it closely matches an existing
 * view (since all data of the existing view is already synced).
 *
 * A view is permanently attached to one specific world.
 *
 * All [View] implementations must support CubicChunks or fail gracefully in its presence.
 */
interface View {
    /**
     * Marks the view as invalid and allows its chunks to be unloaded (if they aren't claimed by other views).
     * [isValid] must return `false` after this method is called.
     */
    fun dispose()

    /**
     * Whether this view is still valid.
     * See [dispose].
     */
    val isValid: Boolean

    /**
     * Check if this view [isValid] and throw an exception if it is not.
     */
    fun checkValid() {
        check(isValid) { "View $this has already been destroyed" }
    }

    /**
     * The worlds manager responsible for this view.
     */
    val manager: ServerWorldsManager

    /**
     * The world of this view.
     */
    val world: WorldServer

    /**
     * Center of this view. Used for determining e.g. which entities to sync.
     * This corresponds to the (virtual) player position.
     */
    val center: Vec3d

    /**
     * Determines which cubes/columns/chunks are visible from the view, i.e. which ones need to be synced.
     */
    val cubeSelector: CubeSelector

    /**
     * Chunk/Cube coordinates of the anchor point of this view.
     * If a view has an anchor point, it will only be active while the anchor is loaded.
     *
     * This allows for detection and unloading of view cycles which, even though they're not visible from the real
     * player, would otherwise recursively keep themselves loaded.
     */
    val anchor: Pair<WorldServer, Vec3i>?

    /**
     * Additional distance added between player and a view when jumping from [anchor] cube to [center] cube.
     * See [CubeSelector.withAnchorDistance].
     * Should be greater than zero to limit recursive view loading. Ignored if [anchor] is `null`.
     */
    val portalDistance: Int
}

/**
 * A simple, stationary view with a [CuboidCubeSelector] target with default view distance.
 *
 * As returned by [ServerWorldsManager.createView].
 */
open class SimpleView(
        final override val manager: ServerWorldsManager,
        final override val world: WorldServer,
        final override val center: Vec3d,
        final override val anchor: Pair<WorldServer, Vec3i>?,
        override val portalDistance: Int = 1
) : View {
    override var isValid: Boolean = true
    override fun dispose() {
        isValid = false
    }

    // TODO somehow update when render distance changes
    override val cubeSelector: CubeSelector = manager.player.server!!.playerList.let { playerList ->
        CuboidCubeSelector(
                center.toBlockPos().toCubePos(),
                playerList.viewDistance,
                if (world.isCubicWorld) playerList.verticalViewDistance else playerList.viewDistance
        )
    }
}

/**
 * Selects cubes based on some criterion.
 *
 * Implementations must be immutable and (preferably cheaply) comparable as that's how the worlds manager determines
 * whether to do a full ([forEachCube]) updated.
 */
interface CubeSelector {
    /**
     * Return a new [CubeSelector] based on this one with the given shortest distance from the player (or any other
     * anchor-less view) to the anchor of this view in cubes (i.e. 16 blocks/meters).
     * I.e. if the player is in the same cube as the anchor of this view, the distance will be 0.
     *
     * Other views are taken into account as if they were providing a [x][View.portalDistance]-distance connection
     * from their [anchor][View.anchor] cube to their [center][View.center] cube (but not the other way around).
     * I.e. if the player is in the same cube as the anchor of a second view and the center of that second view is in
     * the same cube as the anchor of this view, the distance will be [x][View.portalDistance].
     *
     * The purpose is to allow for smooth loading of chunks in portals, i.e. if the portal is already barely within
     * view distance of the player, there's little point in loading further 17x17 chunks, 3x3 is probably enough.
     * A larger anchor distance may select less or the same amount of cubes but **never** more cubes than a smaller
     * anchor distance and vice versa.
     *
     * The given value is the ceil of the euclidean distance between the two chunks/cubes and as such may be larger than
     * the overall view distance (which is a chebyshev distance).
     * If no anchor is set, the distance will always be 0.
     *
     * Given any [CubeSelector] should be immutable, this method may (and should) return `this` if the given anchor
     * distance matches its current one.
     * This method should generally be rather cheap as it may be called multiple times per view graph traversal.
     */
    fun withAnchorDistance(distance: Int): CubeSelector

    /**
     * Whether the specified cube position is included by this selector.
     * Only called for CC worlds.
     */
    fun isCubeIncluded(pos: Vec3i): Boolean

    /**
     * Whether the specified column/chunk position is included by this selector.
     */
    fun isColumnIncluded(pos: ChunkPos): Boolean

    /**
     * Calls the given function with every cube position included by this selector.
     * Only called for CC worlds.
     */
    fun forEachCube(func: (pos: Vec3i) -> Unit)

    /**
     * Calls the given function with every column/chunk position included by this selector.
     */
    fun forEachColumn(func: (pos: ChunkPos) -> Unit)
}

/**
 * Selects chunks based on chebyshev distance to a center chunk. This matches Vanilla and CC behavior.
 * Note that center and view distances are immutable, so a new selector must be created if they need to be changed.
 */
data class CuboidCubeSelector(
        /**
         * The center chunk.
         */
        val center: Vec3i,

        /**
         * Chebyshev distance (in chunks) used to determine which chunks around [center] will be synced to the client.
         * This is usually but not necessarily the server's view-distance.
         * A value of `0` will only sync the chunk at [center].
         */
        val horizontalViewDistance: Int,

        /**
         * When CubicChunks is installed and enabled for the world, this value is used for the vertical view distance.
         * If it is not installed, only [horizontalViewDistance] is used.
         */
        val verticalViewDistance: Int,

        /**
         * See [CubeSelector.withAnchorDistance].
         */
        val anchorDistance: Int = 0
) : CubeSelector {

    val horizontalEffectiveDistance = max(horizontalViewDistance - anchorDistance, 0)
    val verticalEffectiveDistance = max(verticalViewDistance - anchorDistance, 0)

    override fun withAnchorDistance(distance: Int): CubeSelector =
            if (anchorDistance == distance) this else copy(anchorDistance = distance)

    override fun isCubeIncluded(pos: Vec3i): Boolean {
        val dist = center - pos
        return dist.x.absoluteValue <= horizontalEffectiveDistance
                && dist.y.absoluteValue <= verticalEffectiveDistance
                && dist.z.absoluteValue <= horizontalEffectiveDistance
    }

    override fun isColumnIncluded(pos: ChunkPos): Boolean {
        return (center.x - pos.x).absoluteValue <= horizontalEffectiveDistance
                && (center.z - pos.z).absoluteValue <= horizontalEffectiveDistance
    }

    override fun forEachCube(func: (pos: Vec3i) -> Unit) {
        for (x in center.x - horizontalEffectiveDistance .. center.x + horizontalEffectiveDistance) {
            for (y in center.y - verticalEffectiveDistance .. center.y + verticalEffectiveDistance) {
                for (z in center.z - horizontalEffectiveDistance .. center.z + horizontalEffectiveDistance) {
                    func(Vec3i(x, y, z))
                }
            }
        }
    }

    override fun forEachColumn(func: (pos: ChunkPos) -> Unit) {
        for (x in center.x - horizontalEffectiveDistance .. center.x + horizontalEffectiveDistance) {
            for (z in center.z - horizontalEffectiveDistance .. center.z + horizontalEffectiveDistance) {
                func(ChunkPos(x, z))
            }
        }
    }
}