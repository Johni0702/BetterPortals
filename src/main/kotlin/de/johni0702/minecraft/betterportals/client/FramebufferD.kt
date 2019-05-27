package de.johni0702.minecraft.betterportals.client

import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.OpenGlHelper
import net.minecraft.client.renderer.texture.TextureUtil
import net.minecraft.client.shader.Framebuffer
import org.lwjgl.opengl.GL11

/**
 * Regular [Framebuffer] but with a depth texture.
 */
class FramebufferD(width: Int, height: Int): Framebuffer(width, height, false) {
    // Workaround createFramebuffer being called during the super constructor before any initializer would run
    @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
    lateinit var depthTex: Integer
    var depthTexture: Int
        get() = depthTex.toInt()
        private set(value) {
            depthTex = Integer(value)
        }

    override fun createFramebuffer(width: Int, height: Int) {
        super.createFramebuffer(width, height)

        depthTexture = TextureUtil.glGenTextures()
        GlStateManager.bindTexture(depthTexture)
        GlStateManager.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_DEPTH_COMPONENT, width, height, 0, GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT, null)
        OpenGlHelper.glBindFramebuffer(OpenGlHelper.GL_FRAMEBUFFER, framebufferObject)
        OpenGlHelper.glFramebufferTexture2D(OpenGlHelper.GL_FRAMEBUFFER, OpenGlHelper.GL_DEPTH_ATTACHMENT, GL11.GL_TEXTURE_2D, depthTexture, 0)
    }

    override fun deleteFramebuffer() {
        unbindFramebufferTexture()
        unbindFramebuffer()

        if (depthTexture > -1) {
            TextureUtil.deleteTexture(depthTexture)
            depthTexture = -1
        }

        super.deleteFramebuffer()
    }
}