//#if MC>=11400
//$$ package de.johni0702.minecraft.view.impl.mixin;
//$$
//$$ import net.minecraft.client.MouseHelper;
//$$ import org.spongepowered.asm.mixin.Mixin;
//$$ import org.spongepowered.asm.mixin.Shadow;
//$$ import org.spongepowered.asm.mixin.injection.At;
//$$ import org.spongepowered.asm.mixin.injection.Inject;
//$$ import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//$$
//$$ @Mixin(MouseHelper.class)
//$$ public abstract class Mixin_FmlNoGrab {
//$$     @Shadow
//$$     private boolean mouseGrabbed;
//$$
//$$     @Inject(method = "grabMouse", at = @At("HEAD"), cancellable = true)
//$$     private void noGrab(CallbackInfo ci) {
//$$         // Used to be provided by Forge for 1.12.2 and below
//$$         if (Boolean.parseBoolean(System.getProperty("fml.noGrab", "false"))) {
//$$             this.mouseGrabbed = true;
//$$             ci.cancel();
//$$         }
//$$     }
//$$ }
//#endif
