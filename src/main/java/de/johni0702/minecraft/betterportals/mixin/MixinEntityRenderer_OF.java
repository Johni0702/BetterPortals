package de.johni0702.minecraft.betterportals.mixin;

import de.johni0702.minecraft.betterportals.client.renderer.ViewRenderPlan;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.culling.ClippingHelper;
import net.minecraft.client.renderer.culling.Frustum;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EntityRenderer.class)
public abstract class MixinEntityRenderer_OF {
    @Redirect(method = "renderWorldPass", at = @At(value = "NEW"))
    private Frustum createCamera(ClippingHelper clippingHelper) {
        ViewRenderPlan plan = ViewRenderPlan.Companion.getCURRENT();
        if (plan != null) {
            return plan.getCamera();
        }
        return new Frustum(clippingHelper);
    }
}
