package de.johni0702.minecraft.view.impl.mixin;

import de.johni0702.minecraft.view.client.ClientView;
import de.johni0702.minecraft.view.client.ClientViewManager;
import de.johni0702.minecraft.view.impl.ClientViewAPIImpl;
import de.johni0702.minecraft.view.impl.client.ClientViewImpl;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.Entity;
import net.minecraft.profiler.Profiler;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.storage.ISaveHandler;
import net.minecraft.world.storage.WorldInfo;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WorldClient.class)
public abstract class MixinWorldClient extends World  {
    @Shadow @Final private Minecraft mc;

    protected MixinWorldClient(ISaveHandler saveHandlerIn, WorldInfo info, WorldProvider providerIn, Profiler profilerIn, boolean client) {
        super(saveHandlerIn, info, providerIn, profilerIn, client);
    }

    @Inject(method = "getEntityByID", at = @At("HEAD"), cancellable = true)
    private void getPlayerEntityByID(int entityId, CallbackInfoReturnable<Entity> ci) {
        // Note: Cannot get viewManager via mc.viewManager as that'll return null during player swap when mc.player is null
        ClientViewManager viewManager = ClientViewAPIImpl.INSTANCE.getViewManagerImpl$betterportals_view();
        for (ClientView view : viewManager.getViews()) {
            World world = view.getWorld();
            if (world == this) {
                if (viewManager.getActiveView() == view) {
                    if (mc.player == null) {
                        // The case during player swap, see ClientViewImpl.swapThePlayer
                        ci.setReturnValue(super.getEntityByID(entityId));
                    }
                } else {
                    EntityPlayerSP player = ((ClientViewImpl) view).getThePlayer();
                    if (player != null && player.getEntityId() == entityId) {
                        ci.setReturnValue(player);
                    } else {
                        ci.setReturnValue(super.getEntityByID(entityId));
                    }
                }
                return;
            }
        }
    }
}
