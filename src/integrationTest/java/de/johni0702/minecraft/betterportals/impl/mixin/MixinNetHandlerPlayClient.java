package de.johni0702.minecraft.betterportals.impl.mixin;

import de.johni0702.minecraft.betterportals.impl.TestUtilsKt;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.play.server.SPacketCustomPayload;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//#if MC>=11400
//$$ import net.minecraft.util.ResourceLocation;
//#endif

@SideOnly(Side.CLIENT)
@Mixin(NetHandlerPlayClient.class)
public abstract class MixinNetHandlerPlayClient {
    // TODO preprocessor should handle this?
    //#if FABRIC>=1
    //$$ @Inject(method = "onCustomPayload", at = @At("HEAD"), cancellable = true)
    //#else
    @Inject(method = "handleCustomPayload", at = @At("HEAD"), cancellable = true)
    //#endif
    private void onCustomPayload(SPacketCustomPayload packet, CallbackInfo ci) {
        //#if MC>=11400
        //$$ if (new ResourceLocation("betterportals", "testsync").equals(packet.getChannelName())) {
        //#else
        if ("testsync".equals(packet.getChannelName())) {
        //#endif
            ci.cancel();
            TestUtilsKt.clientGotSync();
        }
    }
}
