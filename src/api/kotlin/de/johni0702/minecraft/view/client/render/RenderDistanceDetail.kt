package de.johni0702.minecraft.view.client.render

import kotlin.math.ceil

/**
 * View may be rendered with a different render distance than the main view.
 * Note that this will not affect chunk compilation/upload distance, only rendering, fog, etc.
 *
 * This detail is present on every pass as indicated by the [RenderPass.renderDistanceDetail] extension's type.
 */
class RenderDistanceDetail(
        /**
         * The render distance in blocks during this pass.
         * A `null` value will inherit the user-configured render distance.
         * A value of `0.0` will render only pure fog (in an optimized way).
         */
        var renderDistance: Double? = null
) {
    /**
     * Some parts of MC only support chunk-grained render distance.
     */
    val renderDistanceChunks: Int?
        get() = renderDistance?.let { ceil(it / 16.0).toInt() }
}

var RenderPass.renderDistanceDetail: RenderDistanceDetail
    get() = get()!!
    set(value) { set(value) }
