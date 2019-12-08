package de.johni0702.minecraft.view.impl.mixin;

import de.johni0702.minecraft.view.impl.ClientViewAPIImpl;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MixinMinecraft {
    //#if MC>=11400
    //$$ @Inject(method = "func_213231_b", at = @At("RETURN"))
    //$$ private void resetClientViewManager(CallbackInfo ci) {
    //$$     ClientViewAPIImpl.INSTANCE.getViewManagerImpl().reset();
    //$$ }
    //#endif
}
