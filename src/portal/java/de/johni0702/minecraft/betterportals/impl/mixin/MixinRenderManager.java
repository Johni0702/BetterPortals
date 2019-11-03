package de.johni0702.minecraft.betterportals.impl.mixin;

import de.johni0702.minecraft.betterportals.impl.client.renderer.PortalRendererHooks;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderManager.class)
public abstract class MixinRenderManager {
    // FIXME preprocessor could handle these
    //#if FABRIC>=1
    //$$ @Inject(method = "render(Lnet/minecraft/entity/Entity;DDDFFZ)V", at = @At("HEAD"), cancellable = true)
    //#else
    @Inject(method = "renderEntity", at = @At("HEAD"), cancellable = true)
    //#endif
    private void beforeRenderEntity(Entity entityIn, double x, double y, double z, float yaw, float partialTicks, boolean p_188391_10_, CallbackInfo ci) {
        if (!PortalRendererHooks.INSTANCE.beforeRender(entityIn)) {
            ci.cancel();
        }
    }

    // FIXME preprocessor could handle these
    //#if FABRIC>=1
    //$$ @Inject(method = "render(Lnet/minecraft/entity/Entity;DDDFFZ)V", at = @At("HEAD"), cancellable = true)
    //#else
    @Inject(method = "renderEntity", at = @At("RETURN"))
    //#endif
    private void afterRenderEntity(Entity entityIn, double x, double y, double z, float yaw, float partialTicks, boolean p_188391_10_, CallbackInfo ci) {
        PortalRendererHooks.INSTANCE.afterRender(entityIn);
    }

    @Inject(method = "renderMultipass", at = @At("HEAD"), cancellable = true)
    private void beforeRenderMultipass(Entity entityIn, float partialTicks, CallbackInfo ci) {
        if (!PortalRendererHooks.INSTANCE.beforeRender(entityIn)) {
            ci.cancel();
        }
    }

    @Inject(method = "renderMultipass", at = @At("RETURN"))
    private void afterRenderMultipass(Entity entityIn, float partialTicks, CallbackInfo ci) {
        PortalRendererHooks.INSTANCE.afterRender(entityIn);
    }
}
