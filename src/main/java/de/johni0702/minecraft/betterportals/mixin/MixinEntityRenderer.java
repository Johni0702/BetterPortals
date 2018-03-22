package de.johni0702.minecraft.betterportals.mixin;

import de.johni0702.minecraft.betterportals.client.PostSetupFogEvent;
import de.johni0702.minecraft.betterportals.client.view.ViewEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public abstract class MixinEntityRenderer {
    @Shadow @Final private Minecraft mc;

    @Redirect(method = "renderWorldPass",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GlStateManager;clear(I)V"))
    private void clearIfNotView(int flags) {
        if (!(mc.player instanceof ViewEntity)) {
            GlStateManager.clear(flags);
        }
    }

    @Inject(method = "setupFog", at = @At("RETURN"))
    private void postSetupFogInView(int start, float partialTicks, CallbackInfo ci) {
        MinecraftForge.EVENT_BUS.post(new PostSetupFogEvent());
    }
}
