package de.johni0702.minecraft.view.impl.mixin;

import de.johni0702.minecraft.view.impl.client.render.ViewCameraEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@SideOnly(Side.CLIENT)
@Mixin(EntityPlayerSP.class)
public abstract class MixinEntityPlayerSP {
    // FIXME why does preprocessor not handle this?
    //#if FABRIC>=1
    //$$ @Inject(method = "isMainPlayer", at = @At("HEAD"), cancellable = true)
    //#else
    @Inject(method = "isUser", at = @At("HEAD"), cancellable = true)
    //#endif
    private void isBeingViewedFromThirdPerson(CallbackInfoReturnable<Boolean> ci) {
        if (Minecraft.getMinecraft().getRenderViewEntity() instanceof ViewCameraEntity) {
            ci.setReturnValue(false);
        }
    }
}
