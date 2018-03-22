package de.johni0702.minecraft.betterportals.client

import net.minecraft.client.renderer.GlStateManager
import org.lwjgl.opengl.GL11

/**
 * See MixinGlStateManager
 */
class GlStateManagerState {
    private val alphaState = GlStateManager.alphaState
    private val lightingState = GlStateManager.lightingState
    private val lightState = GlStateManager.lightState
    private val colorMaterialState = GlStateManager.colorMaterialState
    private val blendState = GlStateManager.blendState
    private val depthState = GlStateManager.depthState
    private val fogState = GlStateManager.fogState
    private val cullState = GlStateManager.cullState
    private val polygonOffsetState = GlStateManager.polygonOffsetState
    private val colorLogicState = GlStateManager.colorLogicState
    private val texGenState = GlStateManager.texGenState
    private val clearState = GlStateManager.clearState
    private val stencilState = GlStateManager.stencilState
    private val normalizeState = GlStateManager.normalizeState
    private val textureState = GlStateManager.textureState
    private val rescaleNormalState = GlStateManager.rescaleNormalState
    private val colorMaskState = GlStateManager.colorMaskState
    private val colorState = GlStateManager.colorState
    private val activeTextureUnit = GlStateManager.activeTextureUnit
    private val activeShadeModel = GlStateManager.activeShadeModel

    init {
        // Duplicate current state
        GlStateManager.alphaState = GlStateManager.AlphaState()
        GlStateManager.alphaState.alphaTest.currentState = alphaState.alphaTest.currentState
        GlStateManager.alphaState.func = alphaState.func
        GlStateManager.alphaState.ref = alphaState.ref
        GlStateManager.lightingState = GlStateManager.BooleanState(GL11.GL_LIGHTING)
        GlStateManager.lightingState.currentState = lightingState.currentState
        GlStateManager.lightState = arrayOfNulls(8)
        for (i in 0..7) {
            GlStateManager.lightState[i] = GlStateManager.BooleanState(GL11.GL_LIGHT0 + i)
            GlStateManager.lightState[i].currentState = lightState[i].currentState
        }
        GlStateManager.colorMaterialState = GlStateManager.ColorMaterialState()
        GlStateManager.colorMaterialState.colorMaterial.currentState = colorMaterialState.colorMaterial.currentState
        GlStateManager.colorMaterialState.face = colorMaterialState.face
        GlStateManager.colorMaterialState.mode = colorMaterialState.mode
        GlStateManager.blendState = GlStateManager.BlendState()
        GlStateManager.blendState.blend.currentState = blendState.blend.currentState
        GlStateManager.blendState.dstFactor = blendState.dstFactor
        GlStateManager.blendState.dstFactorAlpha = blendState.dstFactorAlpha
        GlStateManager.blendState.srcFactor = blendState.srcFactor
        GlStateManager.blendState.srcFactorAlpha = blendState.srcFactorAlpha
        GlStateManager.depthState = GlStateManager.DepthState()
        GlStateManager.depthState.depthTest.currentState = depthState.depthTest.currentState
        GlStateManager.depthState.depthFunc = depthState.depthFunc
        GlStateManager.fogState = GlStateManager.FogState()
        GlStateManager.fogState.fog.currentState = fogState.fog.currentState
        GlStateManager.fogState.density = fogState.density
        GlStateManager.fogState.mode = fogState.mode
        GlStateManager.fogState.start = fogState.start
        GlStateManager.fogState.end = fogState.end
        GlStateManager.cullState = GlStateManager.CullState()
        GlStateManager.cullState.cullFace.currentState = cullState.cullFace.currentState
        GlStateManager.cullState.mode = cullState.mode
        GlStateManager.polygonOffsetState = GlStateManager.PolygonOffsetState()
        GlStateManager.polygonOffsetState.polygonOffsetLine.currentState = polygonOffsetState.polygonOffsetLine.currentState
        GlStateManager.polygonOffsetState.polygonOffsetFill.currentState = polygonOffsetState.polygonOffsetFill.currentState
        GlStateManager.polygonOffsetState.factor = polygonOffsetState.factor
        GlStateManager.polygonOffsetState.units = polygonOffsetState.units
        GlStateManager.colorLogicState = GlStateManager.ColorLogicState()
        GlStateManager.colorLogicState.colorLogicOp.currentState = colorLogicState.colorLogicOp.currentState
        GlStateManager.colorLogicState.opcode = colorLogicState.opcode
        GlStateManager.texGenState = GlStateManager.TexGenState()
        copy(GlStateManager.texGenState.q, texGenState.q)
        copy(GlStateManager.texGenState.r, texGenState.r)
        copy(GlStateManager.texGenState.s, texGenState.s)
        copy(GlStateManager.texGenState.t, texGenState.t)
        GlStateManager.clearState = GlStateManager.ClearState()
        GlStateManager.clearState.depth = clearState.depth
        copy(GlStateManager.clearState.color, clearState.color)
        GlStateManager.stencilState = GlStateManager.StencilState()
        GlStateManager.stencilState.fail = stencilState.fail
        GlStateManager.stencilState.func.func = stencilState.func.func
        GlStateManager.stencilState.func.mask = stencilState.func.mask
        GlStateManager.stencilState.mask = stencilState.mask
        GlStateManager.stencilState.zfail = stencilState.zfail
        GlStateManager.stencilState.zpass = stencilState.zpass
        GlStateManager.normalizeState = GlStateManager.BooleanState(2977)
        GlStateManager.normalizeState.currentState = normalizeState.currentState
        GlStateManager.textureState = arrayOfNulls(8)
        for (j in 0..7) {
            GlStateManager.textureState[j] = GlStateManager.TextureState()
            GlStateManager.textureState[j].textureName = textureState[j].textureName
            GlStateManager.textureState[j].texture2DState.currentState = textureState[j].texture2DState.currentState
        }
        GlStateManager.rescaleNormalState = GlStateManager.BooleanState(32826)
        GlStateManager.rescaleNormalState.currentState = rescaleNormalState.currentState
        GlStateManager.colorMaskState = GlStateManager.ColorMask()
        GlStateManager.colorMaskState.alpha = colorMaskState.alpha
        GlStateManager.colorMaskState.red = colorMaskState.red
        GlStateManager.colorMaskState.green = colorMaskState.green
        GlStateManager.colorMaskState.blue = colorMaskState.blue
        GlStateManager.colorState = GlStateManager.Color()
        copy(GlStateManager.colorState, colorState)
    }

    private fun copy(to: GlStateManager.TexGenCoord, from: GlStateManager.TexGenCoord) {
        to.coord = from.coord
        to.param = from.param
        to.textureGen.currentState = from.textureGen.currentState
    }

    private fun copy(to: GlStateManager.Color, from: GlStateManager.Color) {
        to.alpha = from.alpha
        to.red = from.red
        to.green = from.green
        to.blue = from.blue
    }

    fun restore() {
        GlStateManager.alphaState = alphaState
        GlStateManager.lightingState = lightingState
        GlStateManager.lightState = lightState
        GlStateManager.colorMaterialState = colorMaterialState
        GlStateManager.blendState = blendState
        GlStateManager.depthState = depthState
        GlStateManager.fogState = fogState
        GlStateManager.cullState = cullState
        GlStateManager.polygonOffsetState = polygonOffsetState
        GlStateManager.colorLogicState = colorLogicState
        GlStateManager.texGenState = texGenState
        GlStateManager.clearState = clearState
        GlStateManager.stencilState = stencilState
        GlStateManager.normalizeState = normalizeState
        GlStateManager.textureState = textureState
        GlStateManager.rescaleNormalState = rescaleNormalState
        GlStateManager.colorMaskState = colorMaskState
        GlStateManager.colorState = colorState
        GlStateManager.activeTextureUnit = activeTextureUnit
        GlStateManager.activeShadeModel = activeShadeModel
    }
}
