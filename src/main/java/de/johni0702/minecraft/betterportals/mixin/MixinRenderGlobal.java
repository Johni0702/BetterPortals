package de.johni0702.minecraft.betterportals.mixin;

import de.johni0702.minecraft.betterportals.BPConfig;
import de.johni0702.minecraft.betterportals.client.view.ViewChunkRenderDispatcher;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(RenderGlobal.class)
public abstract class MixinRenderGlobal {
    @Redirect(method = "loadRenderers", at = @At(value = "NEW", target = "net/minecraft/client/renderer/chunk/ChunkRenderDispatcher"))
    private ChunkRenderDispatcher createChunkRenderDispatcher() {
        if (BPConfig.improvedChunkRenderDispatcher) {
            return new ViewChunkRenderDispatcher();
        } else {
            return new ChunkRenderDispatcher();
        }
    }
}
