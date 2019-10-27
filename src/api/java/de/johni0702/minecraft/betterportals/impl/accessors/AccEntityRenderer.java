package de.johni0702.minecraft.betterportals.impl.accessors;

import net.minecraft.client.renderer.EntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

//#if MC>=11400
//$$ import net.minecraft.client.renderer.FogRenderer;
//#endif

@Mixin(EntityRenderer.class)
public interface AccEntityRenderer {
    @Accessor
    float getFovModifierHand();
    @Accessor
    void setFovModifierHand(float value);

    @Accessor
    float getFovModifierHandPrev();
    @Accessor
    void setFovModifierHandPrev(float value);

    @Invoker
    void invokeSetupCameraTransform(
            float partialTicks
            //#if MC<11400
            , int pass
            //#endif
    );

    //#if MC>=11400
    //$$ @Accessor
    //$$ FogRenderer getFogRenderer();
    //#else
    @Invoker
    void invokeUpdateFogColor(float partialTicks);
    //#endif
}
