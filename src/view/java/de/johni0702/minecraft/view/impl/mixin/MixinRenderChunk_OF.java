package de.johni0702.minecraft.view.impl.mixin;

import de.johni0702.minecraft.view.impl.compat.OFRenderChunk;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RenderChunk.class)
@SideOnly(Side.CLIENT)
public interface MixinRenderChunk_OF extends OFRenderChunk {
    @Dynamic
    @Accessor(value = "renderChunkNeighboursValid", remap = false)
    RenderChunk[] getRenderChunkNeighbours();

    @Dynamic
    @Accessor(value = "renderChunkNeighboursUpated", remap = false)
    boolean getRenderChunkNeighboursUpdated();
    @Dynamic
    @Accessor(value = "renderChunkNeighboursUpated", remap = false)
    void setRenderChunkNeighboursUpdated(boolean updated);
}
