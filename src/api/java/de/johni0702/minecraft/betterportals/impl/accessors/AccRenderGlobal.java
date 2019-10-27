package de.johni0702.minecraft.betterportals.impl.accessors;

import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RenderGlobal.class)
public interface AccRenderGlobal {
    @Accessor
    ChunkRenderDispatcher getRenderDispatcher();
    @Accessor
    void setRenderDispatcher(ChunkRenderDispatcher value);
}
