package de.johni0702.minecraft.betterportals.impl.transition.mixin;

import de.johni0702.minecraft.betterportals.impl.transition.server.DimensionTransitionHandler;
import net.minecraft.entity.player.EntityPlayerMP;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;

//#if MC>=11400
//$$ import net.minecraft.entity.Entity;
//$$ import net.minecraft.world.dimension.DimensionType;
//$$ import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//#else
import net.minecraft.server.management.PlayerList;
import net.minecraftforge.common.util.ITeleporter;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//#endif

//#if MC>=11400
//$$ @Mixin(ServerPlayerEntity.class)
//#else
@Mixin(PlayerList.class)
//#endif
public abstract class MixinPlayerList {
    //#if MC>=11400
    //$$ @Inject(method = "changeDimension",
    //$$         at = @At("HEAD"), // FIXME should probably go somewhere later (at least after the event)
    //$$         cancellable = true)
    //$$ private void betterPortalPlayerToDimension(DimensionType dimensionIn, CallbackInfoReturnable<Entity> ci) {
    //$$     if (DimensionTransitionHandler.INSTANCE.transferPlayerToDimension((ServerPlayerEntity) (Object) this, dimensionIn)) {
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
