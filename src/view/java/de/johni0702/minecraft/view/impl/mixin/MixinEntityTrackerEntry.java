package de.johni0702.minecraft.view.impl.mixin;

import de.johni0702.minecraft.betterportals.common.ExtensionsKt;
import de.johni0702.minecraft.view.impl.server.ServerWorldManager;
import de.johni0702.minecraft.view.impl.server.ServerWorldsManagerImpl;
import de.johni0702.minecraft.view.server.View;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityTrackerEntry;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static de.johni0702.minecraft.view.impl.ViewAPIImplKt.getWorldsManagerImpl;

@Mixin(EntityTrackerEntry.class)
public abstract class MixinEntityTrackerEntry {
    @Shadow private long encodedPosX;
    @Shadow private long encodedPosY;
    @Shadow private long encodedPosZ;
    @Shadow @Final private int range;
    @Shadow private int maxRange;
    @Shadow @Final private Entity trackedEntity;

    @Redirect(method = "updatePlayerEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/EntityTrackerEntry;isVisibleTo(Lnet/minecraft/entity/player/EntityPlayerMP;)Z"))
    private boolean isVisibleToAnyViewLegacyCC(EntityTrackerEntry entityTrackerEntry, EntityPlayerMP player) {
        // Old CC (970, still latest on CF) didn't use Mixin for this but has a CubicEntityTrackerEntry class which
        // overrides isVisibleTo.
        // So, to have our inject take effect there as well, we directly call it instead of the normal method.
        CallbackInfoReturnable<Boolean> ci = new CallbackInfoReturnable<>("", true);
        isVisibleToAnyView(player, ci);
        return ci.getReturnValue();
    }

    @Inject(method = "isVisibleTo", at = @At("HEAD"), cancellable = true)
    private void isVisibleToAnyView(EntityPlayerMP player, CallbackInfoReturnable<Boolean> ci) {
        ServerWorldsManagerImpl worldsManager = getWorldsManagerImpl(player);
        worldsManager.updateActiveViews();
        ServerWorldManager worldManager = worldsManager.getWorldManagers().get(player.getServerWorld());

        boolean isCubic = ExtensionsKt.isCubicWorld(player.world);
        int range = Math.min(this.range, this.maxRange);
        for (View view : worldManager.getActiveViews()) {
            Vec3d pos = view.getCenter();
            double dx = pos.x - this.encodedPosX / 4096.0D;
            double dy = pos.y - this.encodedPosY / 4096.0D;
            double dz = pos.z - this.encodedPosZ / 4096.0D;
            if (dx >= -range && dx <= range && dz >= -range && dz <= range
                    && (!isCubic || dy >= -range && dy <= range)) {
                ci.setReturnValue(trackedEntity.isSpectatedByPlayer(player));
                return;
            }
        }
        ci.setReturnValue(false);
    }
}
