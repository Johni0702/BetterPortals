package de.johni0702.minecraft.view.client.render

import de.johni0702.minecraft.betterportals.common.*
import net.minecraft.client.renderer.EntityRenderer
import net.minecraft.client.renderer.culling.ClippingHelper
import net.minecraft.client.renderer.culling.Frustum
import net.minecraft.util.math.Vec3d
import javax.vecmath.Matrix4d
import javax.vecmath.Point3d
import kotlin.math.max
import kotlin.math.min

/**
 * A camera through which a view can be rendered.
 */
class Camera(
        /**
         * Frustum used during rendering.
         */
        val frustum: Frustum,

        /**
         * Position of the camera entity's feet in world space.
         *
         * Equals [eyePosition] + entityEyeHeight.
         */
        val feetPosition: Vec3d,

        /**
         * Position of the camera entity's eyes in world space.
         *
         * Outside of [EntityRenderer.setupCameraTransform], MC's code almost exclusively deals with positions relative
         * to this one or to [feetPosition], however the difference to [viewPosition] becomes crucially important when
         * e.g. doing clipping/culling or determining the visible side of an object. In those cases, use that one
         * instead!
         *
         * Note that after [EntityRenderer.setupCameraTransform], rendering something at (0, 0, 0) will render it at
         * [feetPosition], not [eyePosition].
         */
        val eyePosition: Vec3d,

        /**
         * Actual position of the camera in world space, i.e. taking into account e.g. third person mode.
         *
         * Conceptually, this is equal to `inverseModelViewMatrix * (0, 0, 0)`.
         * See [eyePosition].
         */
        val viewPosition: Vec3d,

        /**
         * Rotation of the camera entity.
         * See [eyePosition].
         *
         * x: pitch
         * y: yaw
         * z: roll
         */
        val eyeRotation: Vec3d,

        /**
         * Actual rotation of the camera.
         * See [viewPosition].
         *
         * x: pitch
         * y: yaw
         * z: roll
         */
        val viewRotation: Vec3d
) {
    fun transformed(matrix: Matrix4d): Camera {
        val rotation = matrix.toJX4f().toLwjgl3f().extractRotation()
        return Camera(
                // Note: we **MUST NOT** use the no-args constructor of Frustum since it breaks the global clipping helper
                object : Frustum(ClippingHelper()) {
                    private val inverseMatrix = matrix.inverse()
                    override fun setPosition(xIn: Double, yIn: Double, zIn: Double) {
                        with(inverseMatrix * Point3d(xIn, yIn, zIn)) {
                            frustum.setPosition(x, y, z)
                        }
                    }

                    override fun isBoxInFrustum(
                            minX: Double, minY: Double, minZ: Double,
                            maxX: Double, maxY: Double, maxZ: Double
                    ): Boolean {
                        val min = inverseMatrix * Point3d(minX, minY, minZ)
                        val max = inverseMatrix * Point3d(maxX, maxY, maxZ)
                        return frustum.isBoxInFrustum(
                                min(min.x, max.x), min(min.y, max.y), min(min.z, max.z),
                                max(min.x, max.x), max(min.y, max.y), max(min.z, max.z)
                        )
                    }
                },
                (matrix * feetPosition.toPoint()).toMC(),
                (matrix * eyePosition.toPoint()).toMC(),
                (matrix * viewPosition.toPoint()).toMC(),
                (rotation * eyeRotation.toQuaternion()).toPitchYawRoll(),
                (rotation * viewRotation.toQuaternion()).toPitchYawRoll()
        )
    }

    fun withFrustum(frustum: Frustum): Camera = Camera(frustum, feetPosition, eyePosition, viewPosition, eyeRotation, viewRotation)
}