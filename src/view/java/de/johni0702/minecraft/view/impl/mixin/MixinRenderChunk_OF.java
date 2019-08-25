package de.johni0702.minecraft.view.impl.mixin;

import de.johni0702.minecraft.view.impl.compat.OFRenderChunk;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(RenderChunk.class)
@SideOnly(Side.CLIENT)
public abstract class MixinRenderChunk_OF implements OFRenderChunk {
    @Dynamic
    @Shadow(remap = false)
    private RenderChunk[] renderChunkNeighboursValid;

    @Dynamic
    @Shadow(remap = false)
    private boolean renderChunkNeighboursUpated;

    @NotNull
    @Override
    public RenderChunk[] getRenderChunkNeighbours() {
        return renderChunkNeighboursValid;
    }

    @Override
    public void setRenderChunkNeighboursUpdated(boolean renderChunkNeighboursUpdated) {
        this.renderChunkNeighboursUpated = renderChunkNeighboursUpdated;
    }

    @Override
    public boolean getRenderChunkNeighboursUpdated() {
        return renderChunkNeighboursUpated;
    }
}
