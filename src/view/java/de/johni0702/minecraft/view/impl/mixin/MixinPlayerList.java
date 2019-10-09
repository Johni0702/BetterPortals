package de.johni0702.minecraft.view.impl.mixin;

import de.johni0702.minecraft.view.impl.server.ViewAdvancements;
import de.johni0702.minecraft.view.impl.server.ViewEntity;
import de.johni0702.minecraft.view.impl.server.ViewStatsManager;
import net.minecraft.advancements.PlayerAdvancements;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.Packet;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerList;
import net.minecraft.stats.StatisticsManagerServer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

//#if MC>=11400
//$$ import net.minecraft.world.dimension.DimensionType;
//$$ import net.minecraft.world.server.ServerWorld;
//#else
import net.minecraftforge.common.DimensionManager;
//#endif

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
        //#if MC>=11400
        //$$ for (ServerWorld world : server.getWorlds()) {
        //$$     for (PlayerEntity entity : world.getPlayers()) {
        //$$         if (entity instanceof ViewEntity) {
        //$$             ((ViewEntity) entity).connection.sendPacket(packetIn);
        //$$         }
        //$$     }
        //$$ }
        //#else
        for (Integer dimension : DimensionManager.getIDs()) {
            sendPacketToAllViewsInDimension(packetIn, dimension, ci);
        }
        //#endif
    }

    @Inject(method = "sendPacketToAllPlayersInDimension", at = @At("HEAD"))
    private void sendPacketToAllViewsInDimension(Packet<?> packetIn,
                                                 //#if MC>=11400
                                                 //$$ DimensionType dimension,
                                                 //#else
                                                 int dimension,
                                                 //#endif
                                                 CallbackInfo ci) {
        //#if MC>=11400
        //$$ List<ServerPlayerEntity> players = this.server.getWorld(dimension).getPlayers();
        //#else
        List<EntityPlayer> players = this.mcServer.getWorld(dimension).playerEntities;
        //#endif
        for (EntityPlayer entity : players) {
            if (entity instanceof ViewEntity) {
                ((ViewEntity) entity).connection.sendPacket(packetIn);
            }
        }
    }

    @Inject(method = "sendToAllNearExcept", at = @At("HEAD"))
    private void sendPacketToAllViewsNearExcept(EntityPlayer except,
                                                double x, double y, double z,
                                                double radius,
                                                //#if MC>=11400
                                                //$$ DimensionType dimension,
                                                //#else
                                                int dimension,
                                                //#endif
                                                Packet<?> packetIn,
                                                CallbackInfo ci) {
        //#if MC>=11400
        //$$ List<ServerPlayerEntity> players = this.server.getWorld(dimension).getPlayers();
        //#else
        List<EntityPlayer> players = this.mcServer.getWorld(dimension).playerEntities;
        //#endif
        for (EntityPlayer entity : players) {
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

    @Inject(method = "getPlayerAdvancements", at = @At("HEAD"), cancellable = true)
    private void getPlayerAdvancementsForViewEntity(EntityPlayerMP player, CallbackInfoReturnable<PlayerAdvancements> ci) {
        if (player instanceof ViewEntity) {
            ci.setReturnValue(new ViewAdvancements(this.mcServer, player));
        }
    }

    @Inject(method = "getPlayerStatsFile", at = @At("HEAD"), cancellable = true)
    private void getPlayerStatsForViewEntity(EntityPlayer player, CallbackInfoReturnable<StatisticsManagerServer> ci) {
        if (player instanceof ViewEntity) {
            ci.setReturnValue(new ViewStatsManager(this.mcServer));
        }
    }
}
