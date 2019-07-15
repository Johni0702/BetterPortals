package de.johni0702.minecraft.betterportals.impl.mixin;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.shader.ShaderManager;
import net.minecraft.client.util.JsonBlendingMode;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(JsonBlendingMode.class)
public abstract class MixinJsonBlendingMode {
    @Shadow private static JsonBlendingMode lastApplied;

    @Shadow @Final private boolean opaque;

    @Shadow @Final private int blendFunction;

    @Shadow @Final private int srcColorFactor;

    @Shadow @Final private int destColorFactor;

    @Shadow @Final private int srcAlphaFactor;

    @Shadow @Final private int destAlphaFactor;

    /**
     * For some reason someone at Mojang though it'd be a great idea to skip the GL calls when the previous blend state
     * equals the current one. The thing they forgot about is the rest of the game which doesn't use this class but
     * nevertheless modifies the GL blend state...
     *
     * This assumption is so bad in fact, that even the Shader class itself already breaks it by unconditionally
     * disabling blending before rendering. As a result, a second bug has gone unnoticed: The blend mode of the
     * entity_outline program is incorrect (should be no blending but specifies standard alpha blending and since the
     * last step of the outline pipeline also specifies standard alpha blending, the update for the first step does
     * nothing and keeps the already-disable blending state around).
     *
     * Side note: The lazy GL state updates in GlStateManager.blendFunc seem broken as well when taking
     * GlStateManager.tryBlendFuncSeparate into account.
     *
     * @author johni0702
     */
    @Overwrite
    public void apply() {
        ShaderManager shaderManager = ShaderManager.staticShaderManager;
        if (shaderManager != null && "entity_outline".equals(shaderManager.programFilename)) {
            GlStateManager.disableBlend();
            return;
        }

        if (opaque) {
            GlStateManager.disableBlend();
        } else {
            GlStateManager.enableBlend();
        }

        GlStateManager.glBlendEquation(blendFunction);
        GlStateManager.tryBlendFuncSeparate(srcColorFactor, destColorFactor, srcAlphaFactor, destAlphaFactor);
    }
}
