package de.johni0702.minecraft.view.impl.mixin;

import de.johni0702.minecraft.view.impl.ClientViewAPIImpl;
import de.johni0702.minecraft.view.impl.client.ClientWorldsManagerImpl;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.play.server.SPacketPlayerPosLook;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NetHandlerPlayClient.class)
public class MixinNetHandlerPlayerClient {
    @Shadow private Minecraft gameController;

    @Inject(method = "handlePlayerPosLook", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/EntityPlayer;setPositionAndRotation(DDDFF)V"))
    private void rewindLocalViewChanges(SPacketPlayerPosLook packet, CallbackInfo ci) {
        // If this method gets called while this view is the server's main view (i.e. the client went through a portal
        // but the server doesn't know yet), then the player has been teleported around on the server side and we need
        // to rewind the local portal usage. The server will ignored our UsePortal message because at the time it
        // receives the message we have an open teleport still awaiting confirmation (the one which caused this call).
        ClientWorldsManagerImpl viewManager = ClientViewAPIImpl.INSTANCE.getViewManagerImpl();
        // only when we are what the server sees as the main view
        if (viewManager.getActiveView() == viewManager.getServerMainView()) {
            viewManager.rewindMainView();
        }
    }
}
