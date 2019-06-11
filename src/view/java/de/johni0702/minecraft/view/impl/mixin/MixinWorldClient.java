package de.johni0702.minecraft.view.impl.mixin;

import de.johni0702.minecraft.view.client.ClientView;
import de.johni0702.minecraft.view.client.ClientViewManagerKt;
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
        for (ClientView view : ClientViewManagerKt.getViewManager(mc).getViews()) {
            EntityPlayerSP camera = view.getCamera();
            if (camera.world == this) {
                if (camera.getEntityId() == entityId) {
                    ci.setReturnValue(camera);
                }
                return;
            }
        }
    }
}
