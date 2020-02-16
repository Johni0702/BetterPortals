package de.johni0702.minecraft.view.impl.mixin;

import de.johni0702.minecraft.view.impl.server.ViewEntity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerChunkMap;
import net.minecraft.server.management.PlayerList;
import net.minecraftforge.common.util.ITeleporter;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static de.johni0702.minecraft.view.impl.ViewAPIImplKt.getWorldsManagerImpl;

// For Sponge, see MixinEntityUtil_Sponge
@Mixin(PlayerList.class)
public abstract class MixinPlayerList_NoSponge {
    @Shadow @Final private MinecraftServer mcServer;

    //
    // Non-enhanced third-party transfers.
    // We need to tear down all of our dimensions before the Respawn packet is sent (otherwise the client will no longer
    // be able to uniquely map dimension ids to world instances).
    //
    @Inject(method = "transferPlayerToDimension(Lnet/minecraft/entity/player/EntityPlayerMP;ILnet/minecraftforge/common/util/ITeleporter;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/WorldServer;getDifficulty()Lnet/minecraft/world/EnumDifficulty;"),
            remap = false)
    private void tearDownViewsBeforeRespawnPacket(EntityPlayerMP player, int dimensionIn, ITeleporter teleporter, CallbackInfo ci) {
        getWorldsManagerImpl(player).beforeTransferToDimension(mcServer.getWorld(dimensionIn));
    }

    @Redirect(method = "preparePlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/management/PlayerChunkMap;removePlayer(Lnet/minecraft/entity/player/EntityPlayerMP;)V"))
    private void tearDownViewsBeforeRespawnPacket(PlayerChunkMap playerChunkMap, EntityPlayerMP player) {
        // Moved to beforeTransferToDimension.
    }

    //
    // Some mods might attempt to teleport our view entities, suppress that
    // e.g. https://github.com/Johni0702/BetterPortals/issues/420
    //
    @Inject(method = "transferPlayerToDimension(Lnet/minecraft/entity/player/EntityPlayerMP;ILnet/minecraftforge/common/util/ITeleporter;)V",
            at = @At("HEAD"),
            cancellable = true,
            remap = false)
    private void ignoreIfViewEntity(EntityPlayerMP player, int dimensionIn, ITeleporter teleporter, CallbackInfo ci) {
        if (player instanceof ViewEntity) {
            ci.cancel();
        }
    }
}
