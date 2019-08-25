package de.johni0702.minecraft.view.impl.mixin;

import de.johni0702.minecraft.betterportals.common.ExtensionsKt;
import de.johni0702.minecraft.view.client.ClientViewAPI;
import de.johni0702.minecraft.view.client.render.RenderPass;
import de.johni0702.minecraft.view.impl.compat.OFRenderChunk;
import kotlin.Pair;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.ViewFrustum;
import net.minecraft.client.renderer.chunk.IRenderChunkFactory;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.entity.Entity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.commons.lang3.mutable.MutableInt;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static de.johni0702.minecraft.betterportals.common.ExtensionsKt.isCubicWorld;

// CC also injects into stuff and we want to be able to overrule its decisions
@Mixin(value = ViewFrustum.class, priority = 900)
@SideOnly(Side.CLIENT)
public abstract class MixinViewFrustum {
    @Shadow protected int countChunksX;
    @Shadow protected int countChunksY;
    @Shadow protected int countChunksZ;
    @Shadow @Final protected World world;
    @Shadow public RenderChunk[] renderChunks;
    @Shadow @Final protected RenderGlobal renderGlobal;
    @Shadow protected abstract int getBaseCoordinate(int p_178157_1_, int p_178157_2_, int p_178157_3_);

    private RenderPass prevRootPass;
    private IRenderChunkFactory chunkFactory;
    private Deque<RenderChunk> freeChunks = new ArrayDeque<>();
    private Map<BlockPos, Pair<RenderChunk, MutableInt>> chunkMap = new HashMap<>();
    private Set<BlockPos> unusedChunkArrays = new HashSet<>();
    private Map<BlockPos, RenderChunk[]> chunkArrayCache = new HashMap<>();

    @Inject(method = "createRenderChunks", at = @At("HEAD"), cancellable = true)
    private void createDefaultArray(IRenderChunkFactory renderChunkFactory, CallbackInfo ci) {
        this.chunkFactory = renderChunkFactory;

        this.renderChunks = getOrCreateChunkArray(Vec3d.ZERO);

        ci.cancel();
    }

    private void gcUnusedChunkArrays() {
        for (BlockPos pos : unusedChunkArrays) {
            RenderChunk[] array = chunkArrayCache.remove(pos);
            assert array != null;
            for (RenderChunk chunk : array) {
                unrefChunk(chunk.getPosition());
            }
        }
        unusedChunkArrays.clear();
        unusedChunkArrays.addAll(chunkArrayCache.keySet());
    }

    private void touchChunkArrays(RenderPass renderPass) {
        if (renderPass.getWorld() == world) {
            Vec3d pos = renderPass.getCamera().getFeetPosition();
            if (!isCubicWorld(world)) {
                pos = new Vec3d(pos.x, 8.0, pos.z);
            }
            getOrCreateChunkArray(pos);
        }

        for (RenderPass child : renderPass.getChildren()) {
            touchChunkArrays(child);
        }
    }

    @Inject(method = "updateChunkPositions", at = @At("HEAD"), cancellable = true)
    private void loadChunkArray(double x, double z, CallbackInfo ci) {
        ci.cancel();
        Minecraft mc = Minecraft.getMinecraft();

        RenderPass currRootPass = ClientViewAPI.getInstance().getRenderPassManager(mc).getRoot();
        if (prevRootPass != currRootPass) {
            prevRootPass = currRootPass;
            gcUnusedChunkArrays();
            touchChunkArrays(currRootPass);
        }

        if (isCubicWorld(world)) {
            Entity view = mc.getRenderViewEntity();
            this.renderChunks = getOrCreateChunkArray(ExtensionsKt.getPos(view));
        } else {
            this.renderChunks = getOrCreateChunkArray(new Vec3d(x, 8.0, z));
        }
    }

    private RenderChunk[] getOrCreateChunkArray(Vec3d viewPos) {
        int viewX = MathHelper.floor(viewPos.x) - 8;
        int viewY = MathHelper.floor(viewPos.y) - 8;
        int viewZ = MathHelper.floor(viewPos.z) - 8;
        BlockPos pos = new BlockPos(viewX & ~15, viewY & ~15, viewZ & ~15);
        return getOrCreateChunkArray(pos);
    }

    private RenderChunk[] getOrCreateChunkArray(BlockPos center) {
        unusedChunkArrays.remove(center);
        return chunkArrayCache.computeIfAbsent(center, this::createChunkArray);
    }

    private RenderChunk[] createChunkArray(BlockPos center) {
        int xSizeInChunks = this.countChunksX;
        int ySizeInChunks = this.countChunksY;
        int zSizeInChunks = this.countChunksZ;
        RenderChunk[] array = new RenderChunk[xSizeInChunks * ySizeInChunks * zSizeInChunks];

        int xSizeInBlocks = xSizeInChunks * 16;
        int ySizeInBlocks = ySizeInChunks * 16;
        int zSizeInBlocks = zSizeInChunks * 16;
        boolean isCubic = isCubicWorld(world);

        for(int xIndex = 0; xIndex < xSizeInChunks; xIndex++) {
            int blockX = this.getBaseCoordinate(center.getX(), xSizeInBlocks, xIndex);

            for(int yIndex = 0; yIndex < ySizeInChunks; yIndex++) {
                int blockY;
                if (isCubic) {
                    blockY = this.getBaseCoordinate(center.getY(), ySizeInBlocks, yIndex);
                } else {
                    blockY = yIndex * 16;
                }

                for(int zIndex = 0; zIndex < zSizeInChunks; zIndex++) {
                    int blockZ = this.getBaseCoordinate(center.getZ(), zSizeInBlocks, zIndex);

                    RenderChunk chunk = refChunk(new BlockPos(blockX, blockY, blockZ));
                    array[(zIndex * ySizeInChunks + yIndex) * xSizeInChunks + xIndex] = chunk;
                }
            }
        }

        return array;
    }

    private RenderChunk refChunk(BlockPos pos) {
        Pair<RenderChunk, MutableInt> pair = chunkMap.computeIfAbsent(pos, pos_ -> new Pair<>(allocChunk(pos_), new MutableInt()));
        pair.getSecond().increment();
        return pair.getFirst();
    }

    private void unrefChunk(BlockPos pos) {
        Pair<RenderChunk, MutableInt> pair = chunkMap.get(pos);
        assert pair != null;
        if (pair.getSecond().decrementAndGet() > 0) return;
        chunkMap.remove(pos);

        freeChunk(pair.getFirst());
    }

    private RenderChunk allocChunk(BlockPos pos) {
        RenderChunk chunk = freeChunks.pollFirst();
        if (chunk == null) {
            chunk = chunkFactory.create(world, renderGlobal, 0);
        }
        chunk.setPosition(pos.getX(), pos.getY(), pos.getZ());
        chunk.setNeedsUpdate(false);

        if (chunk instanceof OFRenderChunk) {
            OFRenderChunk ofChunk = (OFRenderChunk) chunk;
            RenderChunk[] neighbours = ofChunk.getRenderChunkNeighbours();
            for (EnumFacing facing : EnumFacing.values()) {
                Pair<RenderChunk, MutableInt> neighbourPair = chunkMap.get(pos.offset(facing, 16));
                if (neighbourPair != null) {
                    RenderChunk neighbour = neighbourPair.getFirst();
                    neighbours[facing.ordinal()] = neighbour;
                    ((OFRenderChunk) neighbour).getRenderChunkNeighbours()[facing.getOpposite().ordinal()] = chunk;
                }
            }
            ofChunk.setRenderChunkNeighboursUpdated(true);
        }

        return chunk;
    }

    private void freeChunk(RenderChunk chunk) {
        chunk.stopCompileTask();
        freeChunks.offerFirst(chunk);

        if (chunk instanceof OFRenderChunk) {
            OFRenderChunk ofChunk = (OFRenderChunk) chunk;
            RenderChunk[] neighbours = ofChunk.getRenderChunkNeighbours();
            for (EnumFacing facing : EnumFacing.values()) {
                RenderChunk neighbour = neighbours[facing.ordinal()];
                if (neighbour != null) {
                    neighbours[facing.ordinal()] = null;
                    ((OFRenderChunk) neighbour).getRenderChunkNeighbours()[facing.getOpposite().ordinal()] = null;
                }
            }
            ofChunk.setRenderChunkNeighboursUpdated(true);
        }
    }

    @Inject(method = "deleteGlResources", at = @At("HEAD"))
    private void deleteGlResources(CallbackInfo ci) {
        for (RenderChunk chunk : freeChunks) {
            chunk.deleteGlResources();
        }
        freeChunks.clear();

        for (Pair<RenderChunk, MutableInt> chunk : chunkMap.values()) {
            chunk.getFirst().deleteGlResources();
        }
        chunkMap.clear();

        chunkArrayCache.clear();
        unusedChunkArrays.clear();
    }

    @Inject(method = "markBlocksForUpdate", at = @At("HEAD"), cancellable = true)
    private void markBlocksForUpdate(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, boolean updateImmediately, CallbackInfo ci) {
        minX &= ~15;
        minY &= ~15;
        minZ &= ~15;
        maxX &= ~15;
        maxY &= ~15;
        maxZ &= ~15;

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = minX; x <= maxX; x += 16) {
            for (int y = minY; y <= maxY; y += 16) {
                for (int z = minZ; z <= maxZ; z += 16) {
                    pos.setPos(x, y, z);
                    Pair<RenderChunk, MutableInt> pair = chunkMap.get(pos);
                    if (pair != null) {
                        pair.getFirst().setNeedsUpdate(updateImmediately);
                    }
                }
            }
        }

        ci.cancel();
    }
}
