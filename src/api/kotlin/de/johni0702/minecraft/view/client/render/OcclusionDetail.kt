package de.johni0702.minecraft.view.client.render

/**
 * Rendering of certain views can be skipped if they were occluded in a previous frame.
 *
 * For that to happen, the occlusion query must have been accessed in the previous frame.
 *
 * This detail is present on every pass as indicated by the [RenderPass.occlusionDetail] extension's type.
 */
class OcclusionDetail(
        occlusionQuery: OcclusionQuery
) {
    /**
     * Whether the query has been accessed since the beginning of this frame.
     */
    var accessed = false

    val occlusionQuery = occlusionQuery
        get() = field.also { accessed = true }

    /**
     * Whether the view was occluded in the previous frame.
     */
    var occluded = false
}

var RenderPass.occlusionDetail: OcclusionDetail
    get() = get()!!
    set(value) { set(value) }
