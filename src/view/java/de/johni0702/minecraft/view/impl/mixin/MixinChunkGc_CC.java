package de.johni0702.minecraft.view.impl.mixin;

import de.johni0702.minecraft.view.impl.server.PlayerCubeMapHandler;
import io.github.opencubicchunks.cubicchunks.core.server.ChunkGc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ChunkGc.class, remap = false)
public abstract class MixinChunkGc_CC {
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void suppressDuringViewSwap(CallbackInfo ci) {
        if (PlayerCubeMapHandler.INSTANCE.getSuppressChunkGc()) {
            ci.cancel();
        }
    }
}
