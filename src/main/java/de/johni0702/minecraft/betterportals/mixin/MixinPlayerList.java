package de.johni0702.minecraft.betterportals.mixin;

import de.johni0702.minecraft.betterportals.server.DimensionTransitionHandler;
import de.johni0702.minecraft.betterportals.server.view.ViewAdvancements;
import de.johni0702.minecraft.betterportals.server.view.ViewEntity;
import de.johni0702.minecraft.betterportals.server.view.ViewStatsManager;
import net.minecraft.advancements.PlayerAdvancements;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.Packet;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerList;
import net.minecraft.stats.StatisticsManagerServer;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.util.ITeleporter;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The player list has some methods used to send packet to all online players.
 * Instead of iterating over players via {@link net.minecraft.world.World#playerEntities}, it has its own list.
 * We can however not add view entities to that list because it is also used to reply to status messages, tab-complete
 * and probably a few other things we don't want view entities to show up in.
 * So, instead we inject into the relevant packet sending methods here and send them to our view entities as well.
 */
@Mixin(PlayerList.class)
public abstract class MixinPlayerList {
    @Shadow @Final private MinecraftServer mcServer;

    @Shadow public abstract void transferEntityToWorld(Entity entityIn, int lastDimension, WorldServer oldWorldIn, WorldServer toWorldIn, ITeleporter teleporter);

    @Inject(method = "sendPacketToAllPlayers", at = @At("HEAD"))
    private void sendPacketToAllViews(Packet<?> packetIn, CallbackInfo ci) {
        for (Integer dimension : DimensionManager.getIDs()) {
            sendPacketToAllViewsInDimension(packetIn, dimension, ci);
        }
    }

    @Inject(method = "sendPacketToAllPlayersInDimension", at = @At("HEAD"))
    private void sendPacketToAllViewsInDimension(Packet<?> packetIn, int dimension, CallbackInfo ci) {
        for (EntityPlayer entity : mcServer.getWorld(dimension).playerEntities) {
            if (entity instanceof ViewEntity) {
                ((ViewEntity) entity).connection.sendPacket(packetIn);
            }
        }
    }

    @Inject(method = "sendToAllNearExcept", at = @At("HEAD"))
    private void sendPacketToAllViewsNearExcept(EntityPlayer except, double x, double y, double z, double radius, int dimension, Packet<?> packetIn, CallbackInfo ci) {
        for (EntityPlayer entity : mcServer.getWorld(dimension).playerEntities) {
            if (entity instanceof ViewEntity && entity != except) {
                double dx = x - entity.posX;
                double dy = y - entity.posY;
                double dz = z - entity.posZ;
                if (dx * dx + dy * dy + dz * dz < radius * radius) {
                    ((ViewEntity) entity).connection.sendPacket(packetIn);
                }
            }
        }
    }

    @Inject(method = "transferPlayerToDimension(Lnet/minecraft/entity/player/EntityPlayerMP;ILnet/minecraftforge/common/util/ITeleporter;)V",
            remap = false,
            at = @At("HEAD"),
            cancellable = true)
    private void betterPortalPlayerToDimension(EntityPlayerMP player, int dimensionIn, ITeleporter teleporter, CallbackInfo ci) {
        DimensionTransitionHandler.INSTANCE.transferPlayerToDimension((PlayerList)(Object)this, player, dimensionIn, teleporter);
        ci.cancel();
    }

    @Inject(method = "getPlayerAdvancements", at = @At("HEAD"), cancellable = true)
    private void getPlayerAdvancementsForViewEntity(EntityPlayerMP player, CallbackInfoReturnable<PlayerAdvancements> ci) {
        if (player instanceof ViewEntity) {
            ci.setReturnValue(new ViewAdvancements(mcServer, player));
        }
    }

    @Inject(method = "getPlayerStatsFile", at = @At("HEAD"), cancellable = true)
    private void getPlayerStatsForViewEntity(EntityPlayer player, CallbackInfoReturnable<StatisticsManagerServer> ci) {
        if (player instanceof ViewEntity) {
            ci.setReturnValue(new ViewStatsManager());
        }
    }
}
