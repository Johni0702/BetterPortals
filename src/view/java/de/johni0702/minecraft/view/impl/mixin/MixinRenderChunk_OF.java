package de.johni0702.minecraft.view.impl.mixin;

import de.johni0702.minecraft.view.impl.compat.OFRenderChunk;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

//#if FABRIC>=1
//$$ import java.lang.reflect.Field;
//#endif

@Mixin(RenderChunk.class)
@SideOnly(Side.CLIENT)
public abstract class MixinRenderChunk_OF implements OFRenderChunk {
    // Optifabric adds these after Mixin parses the class meta, so mixin complains if we try to shadow them.
    //#if FABRIC>=1
    //$$ private ChunkRenderer[] _renderChunkNeighbours;
    //$$
    //$$ @NotNull
    //$$ @Override
    //$$ public ChunkRenderer[] getRenderChunkNeighbours() {
    //$$     if (_renderChunkNeighbours == null) {
    //$$         try {
    //$$             @SuppressWarnings("JavaReflectionMemberAccess")
    //$$             Field field = ChunkRenderer.class.getDeclaredField("renderChunkNeighboursValid");
    //$$             field.setAccessible(true);
    //$$             _renderChunkNeighbours = (ChunkRenderer[]) field.get(this);
    //$$         } catch (IllegalAccessException | NoSuchFieldException e) {
    //$$             throw new RuntimeException(e);
    //$$         }
    //$$     }
    //$$     return _renderChunkNeighbours;
    //$$ }
    //$$
    //$$ private static Field _renderChunkNeighboursUpdated;
    //$$ private static Field renderChunkNeighboursUpdated() {
    //$$     if (_renderChunkNeighboursUpdated == null) {
    //$$         try {
    //$$             @SuppressWarnings("JavaReflectionMemberAccess")
    //$$             Field field = ChunkRenderer.class.getDeclaredField("renderChunkNeighboursUpated");
    //$$             field.setAccessible(true);
    //$$             _renderChunkNeighboursUpdated = field;
    //$$         } catch (NoSuchFieldException e) {
    //$$             throw new RuntimeException(e);
    //$$         }
    //$$     }
    //$$     return _renderChunkNeighboursUpdated;
    //$$ }
    //$$
    //$$ @Override
    //$$ public void setRenderChunkNeighboursUpdated(boolean renderChunkNeighboursUpdated) {
    //$$     try {
    //$$         renderChunkNeighboursUpdated().set(this, renderChunkNeighboursUpdated);
    //$$     } catch (IllegalAccessException e) {
    //$$         throw new RuntimeException(e);
    //$$     }
    //$$ }
    //$$
    //$$ @Override
    //$$ public boolean getRenderChunkNeighboursUpdated() {
    //$$     try {
    //$$         return (boolean) renderChunkNeighboursUpdated().get(this);
    //$$     } catch (IllegalAccessException e) {
    //$$         throw new RuntimeException(e);
    //$$     }
    //$$ }
    //#else
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
    //#endif
}
