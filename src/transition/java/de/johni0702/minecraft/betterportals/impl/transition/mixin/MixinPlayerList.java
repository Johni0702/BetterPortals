package de.johni0702.minecraft.betterportals.impl.transition.mixin;

import de.johni0702.minecraft.betterportals.impl.transition.server.DimensionTransitionHandler;
import net.minecraft.entity.player.EntityPlayerMP;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//#if MC>=11400
//$$ import kotlin.Triple;
//$$ import net.minecraft.entity.Entity;
//$$ import net.minecraft.world.server.ServerWorld;
//$$ import net.minecraft.util.math.Vec3d;
//$$ import net.minecraft.world.dimension.DimensionType;
//$$ import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//#else
import net.minecraft.server.management.PlayerList;
import net.minecraftforge.common.util.ITeleporter;
//#endif

//#if MC>=11400
//$$ @Mixin(ServerPlayerEntity.class)
//#else
@Mixin(PlayerList.class)
//#endif
public abstract class MixinPlayerList {
    //#if MC>=11400
    //$$ @Inject(method = "teleport",
    //$$         at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/ServerPlayerEntity;getServerWorld()Lnet/minecraft/world/server/ServerWorld;"),
    //$$         cancellable = true)
    //$$ private void betterPortalPlayerToDimension(ServerWorld newWorld, double x, double y, double z, float yaw, float pitch, CallbackInfo ci) {
    //$$     if (DimensionTransitionHandler.INSTANCE.transferPlayerToDimension((ServerPlayerEntity) (Object) this, newWorld.dimension.getType(), new Triple<>(new Vec3d(x, y, z), yaw, pitch))) {
    //$$         ci.cancel();
    //$$     }
    //$$ }
    //$$
    //$$ @Inject(method = "changeDimension",
    //$$         at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;getWorld(Lnet/minecraft/world/dimension/DimensionType;)Lnet/minecraft/world/server/ServerWorld;"),
    //$$         cancellable = true)
    //$$ private void betterPortalPlayerToDimension(DimensionType dimensionIn, CallbackInfoReturnable<Entity> ci) {
    //$$     if (DimensionTransitionHandler.INSTANCE.transferPlayerToDimension((ServerPlayerEntity) (Object) this, dimensionIn, null)) {
    //#else
    @Inject(method = "transferPlayerToDimension(Lnet/minecraft/entity/player/EntityPlayerMP;ILnet/minecraftforge/common/util/ITeleporter;)V",
            remap = false,
            at = @At("HEAD"),
            cancellable = true)
    private void betterPortalPlayerToDimension(EntityPlayerMP player, int dimensionIn, ITeleporter teleporter, CallbackInfo ci) {
        if (DimensionTransitionHandler.INSTANCE.transferPlayerToDimension(player, dimensionIn, teleporter)) {
    //#endif
            ci.cancel();
        }
    }
}
