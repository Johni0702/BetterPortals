package de.johni0702.minecraft.betterportals.impl.client

import net.minecraft.util.math.Vec3d
import org.lwjgl.opengl.GL11
import java.nio.ByteBuffer
import java.nio.ByteOrder

internal fun glClipPlane(plane: Int, normal: Vec3d, pointOnPlane: Vec3d) {
    // Plane is where: ax + by + cz + d = 0
    val a = normal.x
    val b = normal.y
    val c = normal.z
    val d = -a * pointOnPlane.x - b * pointOnPlane.y - c * pointOnPlane.z
    val buf = ByteBuffer.allocateDirect(4 * 8)
            .order(ByteOrder.nativeOrder())
            .asDoubleBuffer().put(a).put(b).put(c).put(d)
    buf.flip()
    GL11.glClipPlane(plane, buf)
}
