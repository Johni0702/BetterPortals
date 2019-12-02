package de.johni0702.minecraft.view.impl.mixin;

import de.johni0702.minecraft.view.impl.server.ViewEntity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.WorldServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static de.johni0702.minecraft.view.impl.ViewAPIImplKt.getWorldsManagerImpl;

// Without Sponge, see MixinPlayerList_NoSponge
@Pseudo
@Mixin(targets = "org.spongepowered.common.entity.EntityUtil", remap = false)
public abstract class MixinEntityUtil_Sponge {
    //
    // Non-enhanced third-party transfers.
    // We need to tear down all of our dimensions before the Respawn packet is sent (otherwise the client will no longer
    // be able to uniquely map dimension ids to world instances).
    //
    @Inject(method = "transferPlayerToWorld(Lnet/minecraft/entity/player/EntityPlayerMP;Lorg/spongepowered/api/event/entity/MoveEntityEvent$Teleport;Lnet/minecraft/world/WorldServer;Lorg/spongepowered/common/bridge/world/ForgeITeleporterBridge;)Lnet/minecraft/entity/player/EntityPlayerMP;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/network/play/server/SPacketRespawn;<init>(ILnet/minecraft/world/EnumDifficulty;Lnet/minecraft/world/WorldType;Lnet/minecraft/world/GameType;)V"))
    private static void tearDownViewsBeforeRespawnPacket(EntityPlayerMP player, @Coerce Object event, WorldServer initialToWorld, @Coerce Object teleporter, CallbackInfoReturnable<EntityPlayerMP> ci) {
        getWorldsManagerImpl(player).beforeTransferToDimension();
    }

    //
    // Some mods might attempt to teleport our view entities, suppress that
    // e.g. https://github.com/Johni0702/BetterPortals/issues/420
    //
    @Inject(method = "transferPlayerToWorld(Lnet/minecraft/entity/player/EntityPlayerMP;Lorg/spongepowered/api/event/entity/MoveEntityEvent$Teleport;Lnet/minecraft/world/WorldServer;Lorg/spongepowered/common/bridge/world/ForgeITeleporterBridge;)Lnet/minecraft/entity/player/EntityPlayerMP;",
            at = @At("HEAD"),
            cancellable = true)
    private static void ignoreIfViewEntity(EntityPlayerMP player, @Coerce Object event, WorldServer initialToWorld, @Coerce Object teleporter, CallbackInfoReturnable<EntityPlayerMP> ci) {
        if (player instanceof ViewEntity) {
            ci.cancel();
        }
    }
}
