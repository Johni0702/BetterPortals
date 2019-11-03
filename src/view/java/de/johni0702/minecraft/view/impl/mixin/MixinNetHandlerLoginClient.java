package de.johni0702.minecraft.view.impl.mixin;

import de.johni0702.minecraft.betterportals.impl.accessors.AccNetworkManager;
import de.johni0702.minecraft.view.impl.client.TransactionNettyHandler;
import net.minecraft.client.network.NetHandlerLoginClient;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.login.server.SPacketLoginSuccess;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NetHandlerLoginClient.class)
public class MixinNetHandlerLoginClient {
    @Shadow @Final private NetworkManager networkManager;

    // FIXME preprocessor should handle this
    //#if FABRIC>=1
    //$$ @Inject(method = "onLoginSuccess", at = @At("HEAD"))
    //#else
    @Inject(method = "handleLoginSuccess", at = @At("HEAD"))
    //#endif
    private void onLoginSuccess(SPacketLoginSuccess packetIn, CallbackInfo ci) {
        TransactionNettyHandler.inject(((AccNetworkManager) this.networkManager).getNettyChannel());
    }
}
