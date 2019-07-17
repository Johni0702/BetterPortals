package de.johni0702.minecraft.view.impl.mixin;

import de.johni0702.minecraft.view.impl.server.PlayerCubeMapHandler;
import io.github.opencubicchunks.cubicchunks.core.server.PlayerCubeMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

@Mixin(PlayerCubeMap.class)
public abstract class MixinPlayerCubeMap_CC {
    @Redirect(method = "addPlayer", at = @At(value = "INVOKE", target = "Ljava/util/List;contains(Ljava/lang/Object;)Z", remap = false))
    private boolean forceAddDuringSwap(List list, Object o) {
        if (PlayerCubeMapHandler.INSTANCE.getSwapInProgress()) {
            return true;
        } else {
            return list.contains(o);
        }
    }
}
