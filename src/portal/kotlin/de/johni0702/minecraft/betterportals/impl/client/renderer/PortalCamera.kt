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
    private val clippingHelper = ClippingHelper()

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

            clippingHelper.setPlane(index, pos, p1, p2, aabb.center)
        }
        // We can use neither near (i.e. portal) plane nor far plane (same as regular far plane)
        // The reason we cannot use the portal plane is that MC discovers chunks to be rendered using a flood fill from
        // the camera which would never reach the portal if we used a portal plane and the portal's too far away.
        clippingHelper.frustum[4][0] = 0f
        clippingHelper.frustum[4][1] = 0f
        clippingHelper.frustum[4][2] = 0f
        clippingHelper.frustum[4][3] = 1f
        clippingHelper.frustum[5][0] = 0f
        clippingHelper.frustum[5][1] = 0f
        clippingHelper.frustum[5][2] = 0f
        clippingHelper.frustum[5][3] = 1f
    }

    override fun isBoxInFrustum(minX: Double, minY: Double, minZ: Double, maxX: Double, maxY: Double, maxZ: Double): Boolean {
        return clippingHelper.isBoxInFrustum(minX, minY, minZ, maxX, maxY, maxZ)
                && inner.isBoxInFrustum(minX, minY, minZ, maxX, maxY, maxZ)
    }
}

private fun ClippingHelper.setPlane(index: Int, pointA: Vec3d, pointB: Vec3d, pointC: Vec3d, pointInView: Vec3d) {
    val sideA = pointA - pointB
    val sideB = pointA - pointC
    var normal = sideA.crossProduct(sideB)
    if (normal.dotProduct(pointA) > normal.dotProduct(pointInView)) {
        normal = normal.scale(-1.0)
    }
    setPlane(index, normal, pointA)
}

private fun ClippingHelper.setPlane(index: Int, planeNormal: Vec3d, pointOnPlane: Vec3d) {
    val plane = frustum[index]
    plane[0] = planeNormal.x.toFloat()
    plane[1] = planeNormal.y.toFloat()
    plane[2] = planeNormal.z.toFloat()
    plane[3] = -pointOnPlane.dotProduct(planeNormal).toFloat()
}
