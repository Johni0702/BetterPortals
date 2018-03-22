package de.johni0702.minecraft.betterportals.mixin;

import de.johni0702.minecraft.betterportals.server.view.ViewEntity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.Packet;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerList;
import net.minecraftforge.common.DimensionManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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
}
