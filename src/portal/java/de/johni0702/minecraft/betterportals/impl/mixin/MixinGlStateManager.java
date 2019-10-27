package de.johni0702.minecraft.betterportals.impl.mixin;

import de.johni0702.minecraft.betterportals.impl.client.renderer.GlStateTracker;
import net.minecraft.client.renderer.GlStateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GlStateManager.class)
public abstract class MixinGlStateManager {
    //#if MC>=11400
    //$$ @Inject(method = "fogMode(Lcom/mojang/blaze3d/platform/GlStateManager$FogMode;)V", at = @At("HEAD"))
    //#else
    @Inject(method = "setFog(Lnet/minecraft/client/renderer/GlStateManager$FogMode;)V", at = @At("HEAD"))
    //#endif
    private static void recordFogMode(GlStateManager.FogMode fogMode, CallbackInfo ci) {
        GlStateTracker.INSTANCE.setFogMode(fogMode);
    }

    @Inject(method = "setFogStart", at = @At("HEAD"))
    private static void recordFogStart(float fogStart, CallbackInfo ci) {
        GlStateTracker.INSTANCE.setFogStart(fogStart);
    }

    @Inject(method = "setFogEnd", at = @At("HEAD"))
    private static void recordFog(float fogEnd, CallbackInfo ci) {
        GlStateTracker.INSTANCE.setFogEnd(fogEnd);
    }

    @Inject(method = "clearColor", at = @At("HEAD"))
    private static void recordClearColor(float red, float green, float blue, float alpha, CallbackInfo ci) {
        GlStateTracker.INSTANCE.setClearRed(red);
        GlStateTracker.INSTANCE.setClearGreen(green);
        GlStateTracker.INSTANCE.setClearBlue(blue);
    }
}
