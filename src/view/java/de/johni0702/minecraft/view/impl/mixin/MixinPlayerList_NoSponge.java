package de.johni0702.minecraft.view.impl.mixin;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.management.PlayerList;
import net.minecraftforge.common.util.ITeleporter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static de.johni0702.minecraft.view.impl.ViewAPIImplKt.getWorldsManagerImpl;

// For Sponge, see MixinEntityUtil_Sponge
@Mixin(PlayerList.class)
public abstract class MixinPlayerList_NoSponge {
    //
    // Non-enhanced third-party transfers.
    // We need to tear down all of our dimensions before the Respawn packet is sent (otherwise the client will no longer
    // be able to uniquely map dimension ids to world instances).
    //
    @Inject(method = "transferPlayerToDimension(Lnet/minecraft/entity/player/EntityPlayerMP;ILnet/minecraftforge/common/util/ITeleporter;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/network/play/server/SPacketRespawn;<init>(ILnet/minecraft/world/EnumDifficulty;Lnet/minecraft/world/WorldType;Lnet/minecraft/world/GameType;)V"),
            remap = false)
    private void tearDownViewsBeforeRespawnPacket(EntityPlayerMP player, int dimensionIn, ITeleporter teleporter, CallbackInfo ci) {
        getWorldsManagerImpl(player).beforeTransferToDimension();
    }
}
