package de.johni0702.minecraft.view.impl.mixin;

import de.johni0702.minecraft.view.impl.server.PlayerCubeMapHandler;
import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.api.world.CubeUnWatchEvent;
import io.github.opencubicchunks.cubicchunks.api.world.CubeWatchEvent;
import io.github.opencubicchunks.cubicchunks.api.world.ICubeWatcher;
import io.github.opencubicchunks.cubicchunks.core.server.CubeWatcher;
import io.github.opencubicchunks.cubicchunks.core.world.cube.Cube;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

@Mixin(value = CubeWatcher.class, remap = false)
public abstract class MixinCubeWatcher_CC implements ICubeWatcher {
    @Shadow @Final private ObjectArrayList<EntityPlayerMP> players;

    @Shadow @Nullable private Cube cube;

    @Shadow @Final private CubePos cubePos;

    @Shadow private boolean sentToPlayers;

    @Inject(method = "removePlayer", at = @At("HEAD"), cancellable = true)
    private void suppressRemoveDuringViewSwap(EntityPlayerMP player, CallbackInfo ci) {
        if (!PlayerCubeMapHandler.INSTANCE.getSwapInProgress()) {
            return;
        }
        ci.cancel();

        if (this.players.remove(player) && this.cube != null) {
            MinecraftForge.EVENT_BUS.post(new CubeUnWatchEvent(this.cube, this.cubePos, this, player));
        }
    }

    @Inject(method = "addPlayer", at = @At("HEAD"), cancellable = true)
    private void suppressAddDuringViewSwap(EntityPlayerMP player, CallbackInfo ci) {
        if (!PlayerCubeMapHandler.INSTANCE.getSwapInProgress()) {
            return;
        }
        ci.cancel();

        this.players.add(player);
        if (this.sentToPlayers) {
            MinecraftForge.EVENT_BUS.post(new CubeWatchEvent(this.cube, this.cubePos, this, player));
        }
    }
}
