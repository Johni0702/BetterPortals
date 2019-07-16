package de.johni0702.minecraft.betterportals.common

import net.minecraft.util.math.AxisAlignedBB

/**
 * Common portal runtime configuration options.
 *
 * Note: Default values are subject to change.
 */
class PortalConfiguration @JvmOverloads constructor(
        /**
         * Determines the opacity of the original portal texture.
         * A value of 0 will not render the original texture at all.
         * A value of 1 is maximally opaque, i.e. unchanged from the original value (the remote world will nevertheless be rendered).
         */
        var opacity: () -> Double = { 0.5 },

        /**
         * The distance at which portals will fully render the remote world.
         * Together with [renderDistMin], these settings describe a range in which the portal content fades in.
         * This value is the lower/closer bound of that range, i.e. portal content will be fully visible.
         * If this value is greater than [renderDistMax], [renderDistMax] is used instead.
         *
         * A value between 0 and 1 specifies a fraction of the overall render distance.
         * A value equal or greater than 1 specifies an absolute distance in chunks.
         */
        var renderDistMin: () -> Double = { 1.0 },

        /**
         * The minimum distance at which portals will begin to render the remote world.
         * Together with [renderDistMax], these settings describe a range in which the portal content fades in.
         * This value is the higher/farther bound of that range, i.e. portal content will be mostly foggy.
         *
         * A value between 0 and 1 specifies a fraction of the overall render distance.
         * A value equal or greater than 1 specifies an absolute distance in chunks.
         */
        var renderDistMax: () -> Double = { 3.0 },

        /**
         * For portals which have a side length greater than this value, the [renderDistMin] and [renderDistMax] values
         * will be doubled.
         * If the side length is greater than twice this value, they will be tripled.
         * Greater than thrice this value, they will be quadrupled.
         * Etc.
         *
         * Render dist values between 0 and 1 will be scaled after they've been converted into absolute ones.
         */
        val renderDistSizeMultiplier: () -> Int = { 10 }
) {
        fun getRenderDistMultiplier(portal: AxisAlignedBB): Int = getRenderDistMultiplier(portal.maxSideLength.toInt())
        fun getRenderDistMultiplier(maxSideLength: Int): Int = maxSideLength / renderDistSizeMultiplier() + 1
}