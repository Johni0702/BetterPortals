package de.johni0702.minecraft.view.impl.mixin;

import de.johni0702.minecraft.view.client.render.RenderDistanceDetail;
import de.johni0702.minecraft.view.client.render.RenderPass;
import de.johni0702.minecraft.view.impl.client.render.ViewRenderManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EntityRenderer;
import org.spongepowered.asm.lib.Opcodes;
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

    @Shadow private float farPlaneDistance;

    @Redirect(
            //#if MC>=11400
            //$$ method = "updateCameraAndRender(FJZ)V",
            //#else
            method = "updateCameraAndRender",
            //#endif
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/EntityRenderer;renderWorld(FJ)V")
    )
    private void renderWorld(EntityRenderer entityRenderer, float partialTicks, long finishTimeNano) {
        ViewRenderManager.Companion.getINSTANCE().renderWorld(partialTicks, finishTimeNano);
    }

    @Inject(method = "setupCameraTransform",
            at = @At(value = "FIELD",
                    opcode = Opcodes.PUTFIELD,
                    target = "Lnet/minecraft/client/renderer/EntityRenderer;farPlaneDistance:F",
                    shift = At.Shift.AFTER))
    private void setExactFarPlaneDistance(
            float partialTicks,
            //#if MC<11400
            int pass,
            //#endif
            CallbackInfo ci
    ) {
        RenderPass renderPass = ViewRenderManager.Companion.getINSTANCE().getCurrent();
        if (renderPass == null) return;
        Double distance = renderPass.get(RenderDistanceDetail.class).getRenderDistance();
        if (distance == null) return;
        farPlaneDistance = distance.floatValue();
    }
}
