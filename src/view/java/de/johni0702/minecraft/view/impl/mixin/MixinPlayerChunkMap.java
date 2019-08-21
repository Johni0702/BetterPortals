package de.johni0702.minecraft.view.impl.mixin;

import de.johni0702.minecraft.view.impl.server.ServerWorldManager;
import de.johni0702.minecraft.view.impl.server.ServerWorldsManagerImpl;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.management.PlayerChunkMap;
import net.minecraft.server.management.PlayerChunkMapEntry;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.WorldServer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

import static de.johni0702.minecraft.view.impl.ViewAPIImplKt.getWorldsManagerImpl;

@Mixin(PlayerChunkMap.class)
public abstract class MixinPlayerChunkMap {
    @Shadow private int playerViewRadius;

    @Shadow protected abstract PlayerChunkMapEntry getOrCreateEntry(int chunkX, int chunkZ);
    private PlayerChunkMapEntry getOrCreateEntry(ChunkPos pos) {
        return getOrCreateEntry(pos.x, pos.z);
    }

    @Shadow @Final public List<EntityPlayerMP> players;

    @Shadow protected abstract void markSortPending();

    @Shadow @Nullable public abstract PlayerChunkMapEntry getEntry(int x, int z);

    @Shadow @Final private WorldServer world;

    @Inject(method = "addPlayer", at = @At("HEAD"), cancellable = true)
    private void addPlayerWithViews(EntityPlayerMP player, CallbackInfo ci) {
        ci.cancel();

        player.managedPosX = player.posX;
        player.managedPosZ = player.posZ;

        ServerWorldsManagerImpl worldsManager = getWorldsManagerImpl(player);
        worldsManager.updateActiveViews();
        ServerWorldManager worldManager = worldsManager.getWorldManagers().get(world);
        worldManager.updateTrackedColumns(this::getOrCreateEntry);

        players.add(player);
        markSortPending();
    }

    @Inject(method = "removePlayer", at = @At("HEAD"), cancellable = true)
    private void removePlayerWithViews(EntityPlayerMP player, CallbackInfo ci) {
        ci.cancel();

        ServerWorldsManagerImpl worldsManager = getWorldsManagerImpl(player);
        worldsManager.updateActiveViews();
        ServerWorldManager worldManager = worldsManager.getWorldManagers().get(world);
        for (ChunkPos pos : worldManager.getTrackedColumns()) {
            PlayerChunkMapEntry entry = getEntry(pos.x, pos.z);
            if (entry != null) {
                entry.removePlayer(player);
            }
        }

        players.remove(player);
        markSortPending();
    }

    @Inject(method = "updateMovingPlayer", at = @At("HEAD"), cancellable = true)
    private void updateMovingPlayerWithViews(EntityPlayerMP player, CallbackInfo ci) {
        ci.cancel();

        ServerWorldsManagerImpl worldsManager = getWorldsManagerImpl(player);
        ServerWorldManager worldManager = worldsManager.getWorldManagers().get(world);
        if (worldsManager.getPlayer() == player) {
            double dX = player.managedPosX - player.posX;
            double dZ = player.managedPosZ - player.posZ;
            if (dX * dX + dZ * dZ >= 64.0D) {
                boolean changeOnX = ((int) player.posX >> 4) != ((int) player.managedPosX >> 4);
                boolean changeOnZ = ((int) player.posZ >> 4) != ((int) player.managedPosZ >> 4);
                if (changeOnX || changeOnZ) {
                    player.managedPosX = player.posX;
                    player.managedPosZ = player.posZ;

                    worldsManager.updateActiveViews();
                }
            }
        }

        if (worldManager.getNeedsUpdate()) {
            worldManager.updateTrackedColumns(this::getOrCreateEntry);
            worldManager.setNeedsUpdate(false);
            this.markSortPending();
        }
    }

    @Inject(method = "setPlayerViewRadius", at = @At("HEAD"), cancellable = true)
    private void setPlayerViewRadiusWithViews(int radius, CallbackInfo ci) {
        ci.cancel();

        radius = MathHelper.clamp(radius, 3, 32);
        if (radius == this.playerViewRadius) {
            return;
        }
        this.playerViewRadius = radius;

        for (EntityPlayerMP player : new ArrayList<>(this.players)) {
            ServerWorldsManagerImpl worldsManager = getWorldsManagerImpl(player);
            worldsManager.updateActiveViews();
            ServerWorldManager worldManager = worldsManager.getWorldManagers().get(world);
            worldManager.updateTrackedColumns(this::getOrCreateEntry);
        }

        this.markSortPending();
    }
}
