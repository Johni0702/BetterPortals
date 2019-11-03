package de.johni0702.minecraft.view.impl.mixin;

import net.minecraft.entity.player.EntityPlayerMP;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;

import static de.johni0702.minecraft.view.impl.ViewAPIImplKt.getWorldsManagerImpl;

//#if MC>=11400
//$$ import net.minecraft.entity.Entity;
//$$ import net.minecraft.world.dimension.DimensionType;
//$$ import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//#else
import net.minecraft.server.management.PlayerList;
import net.minecraftforge.common.util.ITeleporter;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//#endif

// For Sponge, see MixinEntityUtil_Sponge
//#if MC>=11400
//$$ @Mixin(ServerPlayerEntity.class)
//#else
@Mixin(PlayerList.class)
//#endif
public abstract class MixinPlayerList_NoSponge {
    //
    // Non-enhanced third-party transfers.
    // We need to tear down all of our dimensions before the Respawn packet is sent (otherwise the client will no longer
    // be able to uniquely map dimension ids to world instances).
    //
    //#if MC>=11400
    //$$ @Inject(method = "changeDimension",
    //$$         at = @At(value = "INVOKE", target = "Lnet/minecraft/network/play/server/SRespawnPacket;<init>(Lnet/minecraft/world/dimension/DimensionType;Lnet/minecraft/world/WorldType;Lnet/minecraft/world/GameType;)V"))
    //$$ private void tearDownViewsBeforeRespawnPacket(DimensionType dimensionType, CallbackInfoReturnable<Entity> ci) {
    //$$     getWorldsManagerImpl((ServerPlayerEntity) (Object) this).beforeTransferToDimension();
    //$$ }
    //#else
    @Inject(method = "transferPlayerToDimension(Lnet/minecraft/entity/player/EntityPlayerMP;ILnet/minecraftforge/common/util/ITeleporter;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/network/play/server/SPacketRespawn;<init>(ILnet/minecraft/world/EnumDifficulty;Lnet/minecraft/world/WorldType;Lnet/minecraft/world/GameType;)V"),
            remap = false)
    private void tearDownViewsBeforeRespawnPacket(EntityPlayerMP player, int dimensionIn, ITeleporter teleporter, CallbackInfo ci) {
        getWorldsManagerImpl(player).beforeTransferToDimension();
    }
    //#endif
}
