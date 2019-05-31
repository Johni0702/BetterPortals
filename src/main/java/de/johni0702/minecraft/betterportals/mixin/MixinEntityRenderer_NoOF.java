package de.johni0702.minecraft.betterportals.mixin;

import de.johni0702.minecraft.betterportals.client.renderer.AbstractRenderPortal;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EntityRenderer.class)
public abstract class MixinEntityRenderer_NoOF {
    @Redirect(method = "renderWorldPass", at = @At(value = "NEW"))
    private Frustum createCamera() {
        Frustum camera = AbstractRenderPortal.Companion.createCamera();
        if (camera == null) {
            camera = new Frustum();
        }
        return camera;
    }
}
