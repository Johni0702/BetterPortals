package de.johni0702.minecraft.betterportals.impl.client.renderer

import de.johni0702.minecraft.betterportals.common.*
import net.minecraft.client.renderer.culling.ClippingHelper
import net.minecraft.client.renderer.culling.Frustum
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.Vec3d

/**
 * Camera which looks at the current world through a portal.
 */
internal class PortalCamera(
        private val portal: Portal,
        private val pos: Vec3d,
        private val inner: Frustum
// Note: we **MUST NOT** use the no-args constructor of Frustum since it breaks the global clipping helper
): Frustum(ClippingHelper()) {
    private val portalPlanes: Array<DoubleArray> = Array(4) { DoubleArray(4) }

    /**
     * The reason why we re-write this is method instead of using [ClippingHelper] is quite complicated.
     * In fact, it's semantically equivalent to the vanilla method if you look at its decompiled source. The
     * difference only shows when you look at the bytecode for the original method (not the FG recompiled one!):
     * If any of the parameters are [Double.POSITIVE_INFINITY]/[Double.NEGATIVE_INFINITY] and one of the frustum
     * planes is 0, the result of multiplying them in `dot` is NaN, so far so good.
     * Then, this method compares the result using `result <= 0.0`, so `NaN <= 0`, which is `false` and this method
     * returns `true` as expected.
     *
     * Now for the reason we re-write this method instead of just using the vanilla one: The vanilla method does
     * its comparision using `!(result > 0)` which might look equivalent but isn't when NaN comes into the mix.
     * As a result the vanilla method returns `false` which isn't what we expect.
     * To make matters worse, there's a bug in fernflower (FG's decompiler) which causes these comparisons to be
     * decompiled incorrectly resulting in the vanilla method working differently in dev compared to live.
     * See: https://github.com/MinecraftForge/ForgeFlower/pull/11 (apparently hasn't made it to FG 2.3 though)
     */
    private fun isBoxInPortalFrustum(minX: Double, minY: Double, minZ: Double, maxX: Double, maxY: Double, maxZ: Double): Boolean =
            portalPlanes.none {
                fun dot(x: Double, y: Double, z: Double): Double = it[0] * x + it[1] * y + it[2] * z + it[3]
                dot(minX, minY, minZ) <= 0.0
                        && dot(maxX, minY, minZ) <= 0.0
                        && dot(minX, maxY, minZ) <= 0.0
                        && dot(maxX, maxY, minZ) <= 0.0
                        && dot(minX, minY, maxZ) <= 0.0
                        && dot(maxX, minY, maxZ) <= 0.0
                        && dot(minX, maxY, maxZ) <= 0.0
                        && dot(maxX, maxY, maxZ) <= 0.0
            }

    override fun setPosition(x: Double, y: Double, z: Double) {
        inner.setPosition(x, y, z)

        // Note: cannot use x,y,z for pos because those are player pos, not camera pos (i.e. feet, not eye level)

        // The (local) direction from which we're looking into the portal
        // i.e. nothing which is in this direction is visible (because it'd be in the remote world)
        val viewFacing = portal.remoteFacing.axis.toFacing(portal.remotePosition.to3dMid() - pos)

        // Side planes
        val aabb = portal.remoteBoundingBox
        viewFacing.axis.parallelFaces.forEachIndexed { index, side ->
            // Construct plane using three points: camera and two corners of the flat aabb per side
            var p1 = Vec3d(aabb.minX, aabb.minY, aabb.minZ)
            var p2 = Vec3d(aabb.maxX, aabb.maxY, aabb.maxZ)

            // Fix one axis to one side
            val sideValue = (if (side.axisDirection == EnumFacing.AxisDirection.NEGATIVE) p1 else p2)[side.axis]
            p1 = p1.with(side.axis, sideValue)
            p2 = p2.with(side.axis, sideValue)

            // Flatten aabb for view axis
            val viewValue = portal.remotePosition.to3dMid()[viewFacing.axis]
            p1 = p1.with(viewFacing.axis, viewValue)
            p2 = p2.with(viewFacing.axis, viewValue)

            portalPlanes[index].setPlane(pos, p1, p2, aabb.center)
        }
        // We can use neither near (i.e. portal) plane nor far plane (same as regular far plane)
        // The reason we cannot use the portal plane is that MC discovers chunks to be rendered using a flood fill from
        // the camera which would never reach the portal if we used a portal plane and the portal's too far away.
        // TODO: the near-plane could now be implemented because [ChunkVisibilityDetail] exists (and is used)
    }

    override fun isBoxInFrustum(minX: Double, minY: Double, minZ: Double, maxX: Double, maxY: Double, maxZ: Double): Boolean {
        return isBoxInPortalFrustum(minX, minY, minZ, maxX, maxY, maxZ)
                && inner.isBoxInFrustum(minX, minY, minZ, maxX, maxY, maxZ)
    }
}

private fun DoubleArray.setPlane(pointA: Vec3d, pointB: Vec3d, pointC: Vec3d, pointInView: Vec3d) {
    val sideA = pointA - pointB
    val sideB = pointA - pointC
    var normal = sideA.crossProduct(sideB)
    if (normal.dotProduct(pointA) > normal.dotProduct(pointInView)) {
        normal = normal.scale(-1.0)
    }
    setPlane(normal, pointA)
}

private fun DoubleArray.setPlane(planeNormal: Vec3d, pointOnPlane: Vec3d) {
    this[0] = planeNormal.x
    this[1] = planeNormal.y
    this[2] = planeNormal.z
    this[3] = -pointOnPlane.dotProduct(planeNormal)
}
