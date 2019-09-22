package de.johni0702.minecraft.betterportals.impl.mixin;

import de.johni0702.minecraft.betterportals.impl.client.PostSetupFogEvent;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//#if MC>=11400
//$$ import net.minecraft.client.renderer.ActiveRenderInfo;
//$$ import net.minecraft.client.renderer.FogRenderer;
//#else
import net.minecraft.client.renderer.EntityRenderer;
//#endif

//#if MC>=11400
//$$ @Mixin(FogRenderer.class)
//#else
@Mixin(EntityRenderer.class)
//#endif
public abstract class Mixin_CallPostSetupFogEvent {
    //#if MC>=11400
    //$$ @Inject(method = "setupFog(Lnet/minecraft/client/renderer/ActiveRenderInfo;IF)V", at = @At("RETURN"), remap = false)
    //$$ private void postSetupFogInView(ActiveRenderInfo p_217618_1_, int p_217618_2_, float partialTicks, CallbackInfo ci) {
    //#else
    @Inject(method = "setupFog", at = @At("RETURN"))
    private void postSetupFogInView(int start, float partialTicks, CallbackInfo ci) {
    //#endif
        MinecraftForge.EVENT_BUS.post(new PostSetupFogEvent());
    }
}
