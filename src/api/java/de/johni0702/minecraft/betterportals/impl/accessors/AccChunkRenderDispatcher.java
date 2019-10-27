package de.johni0702.minecraft.betterportals.impl.accessors;

import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.client.renderer.chunk.ChunkRenderWorker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(ChunkRenderDispatcher.class)
public interface AccChunkRenderDispatcher {
    @Accessor
    List<Thread> getListWorkerThreads();

    @Accessor
    ChunkRenderWorker getRenderWorker();
}
