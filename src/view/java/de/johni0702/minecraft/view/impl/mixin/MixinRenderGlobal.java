package de.johni0702.minecraft.view.impl.mixin;

import de.johni0702.minecraft.view.client.render.ChunkVisibilityDetail;
import de.johni0702.minecraft.view.client.render.RenderPass;
import de.johni0702.minecraft.view.impl.client.render.ViewCameraEntity;
import de.johni0702.minecraft.view.impl.client.render.ViewChunkRenderDispatcher;
import de.johni0702.minecraft.view.impl.client.render.ViewRenderManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.lib.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;

@Mixin(RenderGlobal.class)
public abstract class MixinRenderGlobal {
    @Shadow @Final private Minecraft mc;

    @Redirect(method = "loadRenderers", at = @At(value = "NEW", target = "net/minecraft/client/renderer/chunk/ChunkRenderDispatcher"))
    private ChunkRenderDispatcher createChunkRenderDispatcher() {
        return new ViewChunkRenderDispatcher();
    }

    // See [ChunkVisibilityDetail]
    @Redirect(method = "setupTerrain", at = @At(value = "NEW", target = "net/minecraft/util/math/BlockPos", ordinal = 0))
    private BlockPos getChunkVisibilityFloodFillOrigin(double orgX, double orgY, double orgZ) {
        RenderPass current = ViewRenderManager.Companion.getINSTANCE().getCurrent();
        if (current != null) {
            BlockPos origin = current.get(ChunkVisibilityDetail.class).getOrigin();
            if (origin != null) {
                return origin;
            }
        }
        return new BlockPos(orgX, orgY, orgZ);
    }

    //
    // We'd like to have a lower visual render distance in views whose portals are further away (since you can see less
    // of it anyway). To accomplish that, we set mc.gameSettings.renderDistanceChunks to the appropriate value.
    //
    // However, because that value may fluctuate and RenderGlobal re-creates its view frustum whenever it changes, that
    // would break horribly. So instead we use the real value for the view frustum but the fake value for everything
    // else.
    //

    @Redirect(
            method = "loadRenderers",
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/settings/GameSettings;renderDistanceChunks:I", opcode = Opcodes.GETFIELD)
    )
    private int getRealRenderDistance$0(GameSettings gameSettings) {
        return ViewRenderManager.Companion.getINSTANCE().getRealRenderDistanceChunks();
    }

    @Redirect(
            method = "setupTerrain",
            slice = @Slice(to = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderGlobal;loadRenderers()V")),
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/settings/GameSettings;renderDistanceChunks:I", opcode = Opcodes.GETFIELD)
    )
    private int getRealRenderDistance$1(GameSettings gameSettings) {
        return ViewRenderManager.Companion.getINSTANCE().getRealRenderDistanceChunks();
    }

    @Redirect(
            method = "setupTerrain",
            slice = @Slice(from = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderGlobal;loadRenderers()V")),
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/RenderGlobal;renderDistanceChunks:I", opcode = Opcodes.GETFIELD)
    )
    private int getFakeRenderDistance(RenderGlobal renderGlobal) {
        return mc.gameSettings.renderDistanceChunks;
    }
}
