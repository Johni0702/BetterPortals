package de.johni0702.minecraft.view.impl.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.chunk.ChunkRenderWorker;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ChunkRenderWorker.class)
public abstract class MixinChunkRenderWorker {
    // See the transparency patches in MixinRenderGlobal
    @Redirect(
            method = "processTask",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;getRenderViewEntity()Lnet/minecraft/entity/Entity;")
    )
    private Entity getPlayerZForTransparencySort(Minecraft mc) {
        return mc.player;
    }
}
