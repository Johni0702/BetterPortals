package de.johni0702.minecraft.view.client.render

import net.minecraft.client.renderer.culling.Frustum
import net.minecraft.util.math.Vec3d

/**
 * A camera through which a view can be rendered.
 */
class Camera(
        /**
         * Frustum used during rendering.
         */
        val frustum: Frustum,

        /**
         * Position of the camera in world space.
         */
        val position: Vec3d,

        /**
         * Rotation of the camera.
         *
         * x: pitch
         * y: yaw
         * z: roll
         */
        val rotation: Vec3d
)