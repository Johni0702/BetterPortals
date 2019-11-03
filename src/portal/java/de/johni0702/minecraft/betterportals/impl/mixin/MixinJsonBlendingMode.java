package de.johni0702.minecraft.betterportals.impl.mixin;

import de.johni0702.minecraft.betterportals.impl.accessors.AccShaderManager;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.shader.ShaderManager;
import net.minecraft.client.util.JsonBlendingMode;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;

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
    @Inject(method = "apply", at = @At("HEAD"), cancellable = true)
    private void apply(CallbackInfo ci) throws NoSuchFieldException, IllegalAccessException {
        ci.cancel();

        // TODO
        // The following call throws a NoSuchMethodError on fabric, appears to be somewhat similar (except the other way
        // around) to https://github.com/SpongePowered/Mixin/issues/342 (and indeed, I can reproduce the exact issue in
        // there as well. but that also means that neither works for me atm). I'm assuming this is fixed in Mixin 0.8,
        // so once fabric upgrades, the special case can probably be removed. For now I'm going with the slow
        // option, just to get it running so I can continue working on it.
        //#if FABRIC>=1
        //$$ Field field;
        //$$ try { field = JsonGlProgram.class.getDeclaredField("activeProgram"); }
        //$$ catch(NoSuchFieldException ignored) { field = JsonGlProgram.class.getDeclaredField("field_1512"); }
        //$$ field.setAccessible(true);
        //$$ JsonGlProgram shaderManager = (JsonGlProgram) field.get(null);
        //#else
        ShaderManager shaderManager = AccShaderManager.getStaticShaderManager();
        //#endif
        if (shaderManager != null && "entity_outline".equals(((AccShaderManager) shaderManager).getProgramFilename())) {
            GlStateManager.disableBlend();
            return;
        }

        if (this.opaque) {
            GlStateManager.disableBlend();
        } else {
            GlStateManager.enableBlend();
        }

        GlStateManager.glBlendEquation(this.blendFunction);
        GlStateManager.tryBlendFuncSeparate(this.srcColorFactor, this.destColorFactor, this.srcAlphaFactor, this.destAlphaFactor);
    }
}
