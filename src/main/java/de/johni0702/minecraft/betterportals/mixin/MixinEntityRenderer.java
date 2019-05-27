package de.johni0702.minecraft.betterportals.mixin;

import de.johni0702.minecraft.betterportals.client.PostSetupFogEvent;
import de.johni0702.minecraft.betterportals.client.renderer.AbstractRenderPortal;
import de.johni0702.minecraft.betterportals.client.renderer.ViewRenderManager;
import de.johni0702.minecraft.betterportals.client.view.ViewEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.culling.Frustum;
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

    @Redirect(method = "updateCameraAndRender",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/EntityRenderer;renderWorld(FJ)V"))
    private void renderWorld(EntityRenderer entityRenderer, float partialTicks, long finishTimeNano) {
        ViewRenderManager.Companion.getINSTANCE().renderWorld(finishTimeNano);
    }

    @Redirect(method = "renderWorldPass",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GlStateManager;clear(I)V"))
    private void clearIfNotView(int flags) {
        if (!(mc.player instanceof ViewEntity)) {
            GlStateManager.clear(flags);
        }
    }

    @Redirect(method = "renderWorldPass",
            at = @At(value = "NEW", target = "net/minecraft/client/renderer/culling/Frustum"))
    private Frustum createCamera() {
        Frustum camera = AbstractRenderPortal.Companion.createCamera();
        if (camera == null) {
            camera = new Frustum();
        }
        return camera;
    }

    @Inject(method = "setupFog", at = @At("RETURN"))
    private void postSetupFogInView(int start, float partialTicks, CallbackInfo ci) {
        MinecraftForge.EVENT_BUS.post(new PostSetupFogEvent());
    }
}
