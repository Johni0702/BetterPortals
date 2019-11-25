package de.johni0702.minecraft.betterportals.impl.mixin;

import de.johni0702.minecraft.betterportals.impl.TestUtilsKt;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.play.client.CPacketCustomPayload;
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
@Mixin(NetHandlerPlayServer.class)
public abstract class MixinNetHandlerPlayServer {
    // TODO preprocessor should handle this?
    //#if FABRIC>=1
    //$$ @Inject(method = "onCustomPayload", at = @At("HEAD"), cancellable = true)
    //#else
    @Inject(method = "processCustomPayload", at = @At("HEAD"), cancellable = true)
    //#endif
    private void onCustomPayload(CPacketCustomPayload packet, CallbackInfo ci) {
        //#if MC>=11400
        //#if FABRIC>=1
        //$$ if (true) { // cannot get packet name, so for now assume we're the only once using that packet
        //#else
        //$$ if (new ResourceLocation("betterportals", "testsync").equals(packet.getName())) {
        //#endif
        //#else
        if ("testsync".equals(packet.getChannelName())) {
        //#endif
            ci.cancel();
            TestUtilsKt.serverGotSync();
        }
    }
}
