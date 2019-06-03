package de.johni0702.minecraft.view.client.render

import de.johni0702.minecraft.betterportals.common.popOrNull
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL15
import java.util.ArrayList

class OcclusionQuery {
    companion object {
        private val queries: MutableList<Int> = ArrayList()
    }
    private val queuedQueries: MutableList<Int> = ArrayList()
    var occluded = false

    private fun allocOcclusionQuery(): Int {
        val query = queries.popOrNull() ?: GL15.glGenQueries()
        queuedQueries.add(query)
        return query
    }

    fun begin() {
        GL15.glBeginQuery(GL15.GL_SAMPLES_PASSED, allocOcclusionQuery())
    }

    fun end() {
        GL15.glEndQuery(GL15.GL_SAMPLES_PASSED)
    }

    fun update(): Boolean {
        while (true) {
            val id = queuedQueries.firstOrNull() ?: break
            if (GL15.glGetQueryObjectui(id, GL15.GL_QUERY_RESULT_AVAILABLE) == GL11.GL_TRUE) {
                occluded = GL15.glGetQueryObjectui(id, GL15.GL_QUERY_RESULT) == 0
                queuedQueries.popOrNull()
                queries.add(id)
            } else {
                break
            }
        }
        return queuedQueries.isEmpty()
    }
}