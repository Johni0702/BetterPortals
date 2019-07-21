package de.johni0702.minecraft.view.client.render

import net.minecraft.util.math.BlockPos

/**
 * By default Minecraft determines chunk visibility by flood filling from the view entity's position. This can
 * significantly improve performance by skipping chunks which even though they are in the view frustum cannot possibly
 * be see (e.g. while in a cave).
 * This can however backfire when rendering views where geometry before a particular plane (i.e. portal) is clipped:
 * Even though the chunk would be visible in the render, as far as MC is concerned, it isn't and is therefore culled.
 *
 * To work around this issue, mod should utilize this detail to set a different starting point (e.g. the portal block)
 * for the flood fill process.
 *
 * This detail is present on every pass as indicated by the [RenderPass.chunkVisibilityDetail] extension's type.
 */
class ChunkVisibilityDetail(
        /**
         * The position at which the flood fill will start.
         * A `null` value will use the camera's view position.
         */
        var origin: BlockPos? = null
)

var RenderPass.chunkVisibilityDetail: ChunkVisibilityDetail
    get() = get()!!
    set(value) { set(value) }
