package de.johni0702.minecraft.view.impl.mixin;

import de.johni0702.minecraft.betterportals.common.ExtensionsKt;
import de.johni0702.minecraft.view.impl.server.ServerWorldManager;
import de.johni0702.minecraft.view.impl.server.ServerWorldsManagerImpl;
import de.johni0702.minecraft.view.server.View;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Map;

import static de.johni0702.minecraft.view.impl.ViewAPIImplKt.getWorldsManagerImpl;

//#if MC>=11400
//$$ import net.minecraft.util.math.ChunkPos;
//$$ import net.minecraft.world.TrackedEntity;
//$$ import net.minecraft.world.server.ChunkManager;
//$$ import java.util.Set;
//#else
import net.minecraft.entity.EntityTrackerEntry;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//#endif

//#if MC>=11400
//$$ @Mixin(targets = "net.minecraft.world.server.ChunkManager$EntityTracker")
//#else
@Mixin(EntityTrackerEntry.class)
//#endif
public abstract class MixinEntityTrackerEntry {
    @Shadow @Final private int range;
    //#if MC>=11400
    //$$ @Shadow(aliases = "field_219401_a") @Final private ChunkManager this$0;
    //$$ @Shadow @Final private Entity entity;
    //$$ @Shadow @Final private TrackedEntity entry;
    //$$ @Shadow @Final private Set<ServerPlayerEntity> trackingPlayers;
    //$$
    //$$ @Redirect(method = "updateTrackingState(Lnet/minecraft/entity/player/ServerPlayerEntity;)V",
    //$$         at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/Vec3d;subtract(Lnet/minecraft/util/math/Vec3d;)Lnet/minecraft/util/math/Vec3d;"))
    //$$ private Vec3d alwaysReturnZeroDistance(Vec3d a, Vec3d b) {
    //$$     // Returning zero effectively nullifies MC's range check
    //$$     return Vec3d.ZERO;
    //$$ }
    //$$
    //$$ @Redirect(method = "updateTrackingState(Lnet/minecraft/entity/player/ServerPlayerEntity;)V",
    //$$         // FIXME add srg-named target
    //$$         at = @At(value = "INVOKE", target = "Lnet/minecraft/world/server/ChunkManager;access$500(Lnet/minecraft/util/math/ChunkPos;Lnet/minecraft/entity/player/ServerPlayerEntity;Z)I"))
    //$$ private int getShortestChunkDistToAnyView(ChunkPos chunkPos, ServerPlayerEntity player, boolean useManagedPos) {
    //$$     // not entirely sure why MC has this extra check, afaict we've already checked the distance in
    //$$     // isVisibleToAnyView, so we just force this condition to always be true.
    //$$     return 0;
    //$$ }
    //$$
    //$$ @Redirect(method = "updateTrackingState(Lnet/minecraft/entity/player/ServerPlayerEntity;)V",
    //$$         at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;isSpectatedByPlayer(Lnet/minecraft/entity/player/ServerPlayerEntity;)Z"))
    //$$ private boolean isVisibleToAnyView(Entity entity, ServerPlayerEntity player) {
    //$$     ServerWorldsManagerImpl worldsManager = getWorldsManagerImpl(player);
    //$$     ServerWorldManager worldManager = worldsManager.getWorldManagers().get(player.getServerWorld());
    //$$     if (worldManager == null) {
    //$$         // May be the case when the player is first added to the world during non-enhanced third-party transition
    //$$         // Note: Not sure if this is still the case (was in 1.12.2)
    //$$         return false;
    //$$     }
    //$$
    //$$     boolean isCubic = ExtensionsKt.isCubicWorld(player.world);
    //$$     int baseRange = Math.min(this.range, (((AccessorChunkManager) this$0).getViewDistance() - 1) * 16);
    //$$     for (Map.Entry<View, Integer> entry : worldManager.getActiveViews().entrySet()) {
    //$$         Vec3d pos = entry.getKey().getCenter();
    //$$         int range = Math.max(0, baseRange - entry.getValue() * 16);
    //$$         Vec3d diff = pos.subtract(this.entry.func_219456_b());
    //$$         if (diff.x >= -range && diff.x <= range && diff.z >= -range && diff.z <= range
    //$$                 && (!isCubic || diff.y >= -range && diff.y <= range)) {
    //$$             return entity.isSpectatedByPlayer(player);
    //$$         }
    //$$     }
    //$$     return false;
    //$$ }
    //#else
    @Shadow private long encodedPosX;
    @Shadow private long encodedPosY;
    @Shadow private long encodedPosZ;
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
        ServerWorldManager worldManager = worldsManager.getWorldManagers().get(player.getServerWorld());
        if (worldManager == null) {
            // May be the case when the player is first added to the world during non-enhanced third-party transition
            ci.setReturnValue(false);
            return;
        }

        boolean isCubic = ExtensionsKt.isCubicWorld(player.world);
        int baseRange = Math.min(this.range, this.maxRange);
        for (Map.Entry<View, Integer> entry : worldManager.getActiveViews().entrySet()) {
            Vec3d pos = entry.getKey().getCenter();
            int range = Math.max(0, baseRange - entry.getValue() * 16);
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
    //#endif
}
