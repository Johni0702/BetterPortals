package de.johni0702.minecraft.view.impl.mixin;

import de.johni0702.minecraft.view.impl.server.PlayerCubeMapHandler;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.management.PlayerChunkMap;
import net.minecraft.server.management.PlayerChunkMapEntry;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.ChunkWatchEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(targets = "io.github.opencubicchunks.cubicchunks.core.server.ColumnWatcher", remap = false)
public abstract class MixinColumnWatcher_CC extends PlayerChunkMapEntry {
    public MixinColumnWatcher_CC(PlayerChunkMap mapIn, int chunkX, int chunkZ) {
        super(mapIn, chunkX, chunkZ);
    }

    @Shadow protected abstract List<EntityPlayerMP> getPlayers();

    @Inject(method = "removePlayer", at = @At("HEAD"), cancellable = true, remap = true)
    private void suppressRemoveDuringViewSwap(EntityPlayerMP player, CallbackInfo ci) {
        if (!PlayerCubeMapHandler.INSTANCE.getSwapInProgress()) {
            return;
        }
        ci.cancel();

        if (this.getPlayers().remove(player) && this.getChunk() != null) {
            MinecraftForge.EVENT_BUS.post(new ChunkWatchEvent.UnWatch(this.getChunk(), player));
        }
    }

    @Inject(method = "addPlayer", at = @At("HEAD"), cancellable = true, remap = true)
    private void suppressAddDuringViewSwap(EntityPlayerMP player, CallbackInfo ci) {
        if (!PlayerCubeMapHandler.INSTANCE.getSwapInProgress()) {
            return;
        }
        ci.cancel();

        this.getPlayers().add(player);
        if (this.isSentToPlayers()) {
            MinecraftForge.EVENT_BUS.post(new ChunkWatchEvent.Watch(this.getChunk(), player));
        }
    }
}
