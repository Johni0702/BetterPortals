package de.johni0702.minecraft.betterportals.mixin;

import de.johni0702.minecraft.betterportals.client.renderer.PortalRendererHooks;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderManager.class)
public abstract class MixinRenderManager {
    @Inject(method = "renderEntity", at = @At("HEAD"), cancellable = true)
    private void beforeRenderEntity(Entity entityIn, double x, double y, double z, float yaw, float partialTicks, boolean p_188391_10_, CallbackInfo ci) {
        if (!PortalRendererHooks.INSTANCE.beforeRender(entityIn)) {
            ci.cancel();
        }
    }

    @Inject(method = "renderEntity", at = @At("RETURN"))
    private void afterRenderEntity(Entity entityIn, double x, double y, double z, float yaw, float partialTicks, boolean p_188391_10_, CallbackInfo ci) {
        PortalRendererHooks.INSTANCE.afterRender(entityIn);
    }
}
