package de.johni0702.minecraft.betterportals.impl.accessors;

import net.minecraft.client.renderer.chunk.ChunkCompileTaskGenerator;
import net.minecraft.client.renderer.chunk.ChunkRenderWorker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ChunkRenderWorker.class)
public interface AccChunkRenderWorker {
    @Invoker
    void invokeProcessTask(final ChunkCompileTaskGenerator generator) throws InterruptedException;
}
