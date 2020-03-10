package de.johni0702.minecraft.view.impl.mixin;

import de.johni0702.minecraft.view.impl.ClientViewAPIImpl;
import de.johni0702.minecraft.view.impl.client.ClientWorldsManagerImpl;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.PacketThreadUtil;
import net.minecraft.network.play.INetHandlerPlayClient;
import net.minecraft.network.play.server.SPacketKeepAlive;
import net.minecraft.network.play.server.SPacketPlayerPosLook;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//#if MC>=11400
//$$ import de.johni0702.minecraft.view.impl.net.Net;
//$$ import net.minecraft.network.NetworkManager;
//$$ import net.minecraft.network.play.server.SCustomPayloadPlayPacket;
//#if FABRIC<=0
//$$ import net.minecraftforge.fml.network.NetworkHooks;
//#endif
//#endif

@Mixin(NetHandlerPlayClient.class)
public abstract class MixinNetHandlerPlayerClient implements INetHandlerPlayClient {
    @Shadow private Minecraft gameController;

    // FIXME why does the preprocessor not handle these?
    //#if FABRIC>=1
    //$$ @Inject(method = "onPlayerPositionLook", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;setPositionAnglesAndUpdate(DDDFF)V"))
    //#else
    @Inject(method = "handlePlayerPosLook", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/EntityPlayer;setPositionAndRotation(DDDFF)V"))
    //#endif
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

    // FIXME why does the preprocessor not handle these?
    //#if FABRIC>=1
    //$$ @Inject(method = "onKeepAlive", at = @At(value = "HEAD"))
    //#else
    @Inject(method = "handleKeepAlive", at = @At(value = "HEAD"))
    //#endif
    private void forceKeepAliveToSyncWithMainThread(SPacketKeepAlive packet, CallbackInfo ci) {
        // Keep alive packets are handled on the network thread (presumably just case it doesn't strictly need to
        // access MC state). This can cause issues since there's no guarantee that the right view will be active
        // when sending the reply and as a result the packet may get lost.
        PacketThreadUtil.checkThreadAndEnqueue(packet, this, this.gameController);
    }

    //#if FABRIC<=0
    //#if MC>=11400
    //$$ @Shadow public NetworkManager netManager;
    //$$
    //$$ // View packets need to be handled on the network thread since most of them need to be executed under specific
    //$$ // context (and in most cases without the current-view-wrapper).
    //$$ // This used to be the default in Forge 1.12 but has apparently changed in 1.14. This inject special-cases our
    //$$ // packets and reverts back to the 1.12 behavior for them.
    //$$ @Inject(method = "handleCustomPayload", at = @At("HEAD"), cancellable = true)
    //$$ private void handleViewPacketsOnNetworkThread(SCustomPayloadPlayPacket packet, CallbackInfo ci) {
    //$$     if (Net.CHANNEL.equals(packet.getChannelName())) {
    //$$         NetworkHooks.onCustomPayload(packet, this.netManager);
    //$$         ci.cancel();
    //$$     }
    //$$ }
    //#endif
    //#endif
}
