package de.johni0702.minecraft.view.impl.mixin;

import de.johni0702.minecraft.view.impl.client.render.ViewCameraEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SideOnly(Side.CLIENT)
@Mixin(EntityRenderer.class)
public abstract class MixinEntityRenderer_VC {
    @Shadow @Final private Minecraft mc;

    // No thanks, BP is perfectly capable of rendering from a non-standard angle all by itself.
    @Dynamic
    @Inject(method = {"setupRVE", "cacheRVEPos", "restoreRVEPos"}, at = @At("HEAD"), cancellable = true, remap = false)
    private void skipRVEHackWhenBPIsActive(CallbackInfo ci) {
        if (mc.getRenderViewEntity() instanceof ViewCameraEntity) {
            ci.cancel();
        }
    }
}
