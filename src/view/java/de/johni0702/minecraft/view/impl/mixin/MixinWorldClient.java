package de.johni0702.minecraft.view.impl.mixin;

import de.johni0702.minecraft.view.impl.ClientViewAPIImpl;
import de.johni0702.minecraft.view.impl.client.ClientState;
import de.johni0702.minecraft.view.impl.client.ClientWorldsManagerImpl;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.Entity;
import net.minecraft.profiler.Profiler;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.storage.ISaveHandler;
import net.minecraft.world.storage.WorldInfo;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;

@Mixin(WorldClient.class)
@SideOnly(Side.CLIENT)
public abstract class MixinWorldClient extends World  {
    @Shadow @Final private Minecraft mc;

    @Shadow @Final private Set<Entity> entitySpawnQueue;

    protected MixinWorldClient(ISaveHandler saveHandlerIn, WorldInfo info, WorldProvider providerIn, Profiler profilerIn, boolean client) {
        super(saveHandlerIn, info, providerIn, profilerIn, client);
    }

    @Inject(method = "getEntityByID", at = @At("HEAD"), cancellable = true)
    private void getPlayerEntityByID(int entityId, CallbackInfoReturnable<Entity> ci) {
        // Note: Cannot get worldsManager via mc.worldsManager as that'll return null during player swap when mc.player is null
        ClientWorldsManagerImpl viewManager = ClientViewAPIImpl.INSTANCE.getViewManagerImpl$betterportals_view();
        for (ClientState view : viewManager.getViews()) {
            World world = view.getWorld();
            if (world == this) {
                if (viewManager.getActiveView() == view) {
                    if (mc.player == null) {
                        // The case during player swap, see ClientViewImpl.swapThePlayer
                        ci.setReturnValue(super.getEntityByID(entityId));
                    }
                } else {
                    EntityPlayerSP player = view.getThePlayer();
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

    //
    // Fixing a vanilla bug which leaves entities in the [entitySpawnQueue] even if a destroy packet is sent for them.
    // These entities can clog up the spawn queue and once the first 10 are stuck in there, any other legitimate
    // entities get stuck forever as well even if their chunk is loaded.
    //

    @Inject(method = "removeEntity", at = @At("HEAD"))
    private void removeEntityFromSpawnQueue(Entity entity, CallbackInfo ci) {
        this.entitySpawnQueue.remove(entity);
    }
}
