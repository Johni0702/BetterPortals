package de.johni0702.minecraft.view.impl.mixin;

import de.johni0702.minecraft.view.impl.compat.OFVertexBuffer;
import de.johni0702.minecraft.view.impl.compat.OFViewFrustum;
import kotlin.Pair;
import net.minecraft.client.renderer.ViewFrustum;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.optifine.render.VboRegion;
import org.apache.commons.lang3.mutable.MutableInt;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

@Mixin(ViewFrustum.class)
@SideOnly(Side.CLIENT)
public abstract class MixinViewFrustum_OF implements OFViewFrustum {
    private boolean vboRegionsEnabled = queryVboRegionsEnabled();
    private Deque<VboRegion[]> freeVboRegions = new ArrayDeque<>();
    private Map<BlockPos, Pair<VboRegion[], MutableInt>> vboRegions = new HashMap<>();

    private BlockPos getRegionPos(RenderChunk chunk) {
        BlockPos p = chunk.getPosition();
        return new BlockPos(p.getX() & ~255, p.getY() & ~255, p.getZ() & ~255);
    }

    @Override
    public void refVboRegion(RenderChunk chunk) {
        if (!vboRegionsEnabled) return;
        Pair<VboRegion[], MutableInt> pair = vboRegions.computeIfAbsent(getRegionPos(chunk), pos_ -> new Pair<>(allocVboRegion(), new MutableInt()));
        pair.getSecond().increment();
        VboRegion[] array = pair.getFirst();
        for (BlockRenderLayer layer : BlockRenderLayer.values()) {
            ((OFVertexBuffer) chunk.getVertexBufferByLayer(layer.ordinal())).setVboRegion(array[layer.ordinal()]);
        }
    }

    @Override
    public void unrefVboRegion(RenderChunk chunk) {
        if (!vboRegionsEnabled) return;
        BlockPos pos = getRegionPos(chunk);
        Pair<VboRegion[], MutableInt> pair = vboRegions.get(pos);
        assert pair != null;
        if (pair.getSecond().decrementAndGet() > 0) return;
        vboRegions.remove(pos);

        freeVboRegion(pair.getFirst());
    }

    private VboRegion[] allocVboRegion() {
        VboRegion[] region = freeVboRegions.pollFirst();
        if (region == null) {
            region = new VboRegion[BlockRenderLayer.values().length];
            for (BlockRenderLayer layer : BlockRenderLayer.values()) {
                region[layer.ordinal()] = new VboRegion(layer);
            }
        }
        return region;
    }

    private void freeVboRegion(VboRegion[] region) {
        freeVboRegions.offerFirst(region);
    }

    @Inject(method = "deleteGlResources", at = @At("HEAD"))
    private void deleteGlResources(CallbackInfo ci) {
        for (VboRegion[] array : freeVboRegions) {
            for (VboRegion region : array) {
                region.deleteGlBuffers();
            }
        }
        freeVboRegions.clear();

        for (Pair<VboRegion[], MutableInt> pair : vboRegions.values()) {
            for (VboRegion region : pair.getFirst()) {
                region.deleteGlBuffers();
            }
        }
        vboRegions.clear();
    }

    private static boolean queryVboRegionsEnabled() {
        try {
            Class<?> config = Class.forName("Config");
            boolean isVbo = (Boolean) config.getDeclaredMethod("isVbo").invoke(null);
            boolean isRenderRegions = (Boolean) config.getDeclaredMethod("isRenderRegions").invoke(null);
            return isVbo && isRenderRegions;
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
}
