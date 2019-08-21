package de.johni0702.minecraft.view.server

import de.johni0702.minecraft.betterportals.common.minus
import net.minecraft.util.math.ChunkPos
import net.minecraft.util.math.Vec3d
import net.minecraft.util.math.Vec3i
import net.minecraft.world.WorldServer
import kotlin.math.absoluteValue

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
}

interface CubeSelector {
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
        private val center: Vec3i,

        /**
         * Chebyshev distance (in chunks) used to determine which chunks around [center] will be synced to the client.
         * This is usually but not necessarily the server's view-distance.
         * A value of `0` will only sync the chunk at [center].
         */
        private val horizontalViewDistance: Int,

        /**
         * When CubicChunks is installed and enabled for the world, this value is used for the vertical view distance.
         * If it is not installed, only [horizontalViewDistance] is used.
         */
        private val verticalViewDistance: Int
) : CubeSelector {

    override fun isCubeIncluded(pos: Vec3i): Boolean {
        val dist = center - pos
        return dist.x.absoluteValue <= horizontalViewDistance
                && dist.y.absoluteValue <= verticalViewDistance
                && dist.z.absoluteValue <= horizontalViewDistance
    }

    override fun isColumnIncluded(pos: ChunkPos): Boolean {
        return (center.x - pos.x).absoluteValue <= horizontalViewDistance
                && (center.z - pos.z).absoluteValue <= horizontalViewDistance
    }

    override fun forEachCube(func: (pos: Vec3i) -> Unit) {
        for (x in center.x - horizontalViewDistance .. center.x + horizontalViewDistance) {
            for (y in center.y - verticalViewDistance .. center.y + verticalViewDistance) {
                for (z in center.z - horizontalViewDistance .. center.z + horizontalViewDistance) {
                    func(Vec3i(x, y, z))
                }
            }
        }
    }

    override fun forEachColumn(func: (pos: ChunkPos) -> Unit) {
        for (x in center.x - horizontalViewDistance .. center.x + horizontalViewDistance) {
            for (z in center.z - horizontalViewDistance .. center.z + horizontalViewDistance) {
                func(ChunkPos(x, z))
            }
        }
    }
}