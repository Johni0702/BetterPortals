package de.johni0702.minecraft.view.impl.mixin;

import de.johni0702.minecraft.view.client.render.ChunkVisibilityDetail;
import de.johni0702.minecraft.view.client.render.RenderPass;
import de.johni0702.minecraft.view.impl.client.render.ViewCameraEntity;
import de.johni0702.minecraft.view.impl.client.render.ViewChunkRenderDispatcher;
import de.johni0702.minecraft.view.impl.client.render.ViewRenderManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.lib.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderGlobal.class)
public abstract class MixinRenderGlobal {
    @Shadow @Final private Minecraft mc;

    @Shadow public abstract void loadRenderers();

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
    // MC and other mods call `loadRenderers()` when some graphic settings have changed and chunks need to be rebuilt.
    // In order to propagate these calls to other render globals, a static counter is incremented on each of them and
    // any RenderGlobal which has a private counter that doesn't match the global one is refreshed on the next call to
    // `setupTerrain()`. Not very idiomatic but very pragmatic.
    //

    private static int globalRefreshCount = 0;
    private int refreshCount = globalRefreshCount;
    private boolean ignoreRefresh = false;

    @Inject(method = "loadRenderers", at = @At("HEAD"))
    private void refreshOtherViews(CallbackInfo ci) {
        if (!ignoreRefresh) {
            globalRefreshCount++;
            refreshCount = globalRefreshCount;
        }
    }

    // Ignore calls from setWorldAndLoadRenderers (i.e. when the local world changes).
    @Inject(method = "setWorldAndLoadRenderers", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderGlobal;loadRenderers()V"))
    private void beforeLoadRenderers(WorldClient world, CallbackInfo ci) {
        ignoreRefresh = true;
    }
    @Inject(method = "setWorldAndLoadRenderers", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderGlobal;loadRenderers()V", shift = At.Shift.AFTER))
    private void afterLoadRenderers(WorldClient world, CallbackInfo ci) {
        ignoreRefresh = false;
    }

    @Inject(method = "setupTerrain", at = @At("HEAD"))
    private void refreshIfOtherViewRefreshed(Entity viewEntity, double partialTicks, ICamera camera, int frameCount, boolean playerSpectator, CallbackInfo ci) {
        if (refreshCount != globalRefreshCount) {
            refreshCount = globalRefreshCount;
            ignoreRefresh = true;
            loadRenderers();
            ignoreRefresh = false;
        }
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

    //
    // For rendering views, we use a special ViewCameraEntity which may be positioned anywhere nearby but which
    // will keep the view frustum centered on the player entity (the ViewEntity synced with the server).
    // This allows views to be rendered from arbitrary locations without causing chunks to be unloaded.
    //
    // To accomplish that, the following redirectors will query the player for posX/Y/Z and chunkCoordX/Y/Z if the
    // view entity is a ViewCameraEntity, otherwise they'll behave as normal.
    //

    @Redirect(
            method = "setupTerrain",
            slice = @Slice(to = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ViewFrustum;updateChunkPositions(DD)V")),
            at = @At(value = "FIELD", target = "Lnet/minecraft/entity/Entity;posX:D", opcode = Opcodes.GETFIELD)
    )
    private double getCameraPosX(Entity entity) {
        if (entity instanceof ViewCameraEntity) {
            return mc.player.posX;
        } else {
            return entity.posX;
        }
    }

    @Redirect(
            method = "setupTerrain",
            slice = @Slice(to = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ViewFrustum;updateChunkPositions(DD)V")),
            at = @At(value = "FIELD", target = "Lnet/minecraft/entity/Entity;posY:D", opcode = Opcodes.GETFIELD)
    )
    private double getCameraPosY(Entity entity) {
        if (entity instanceof ViewCameraEntity) {
            return mc.player.posY;
        } else {
            return entity.posY;
        }
    }

    @Redirect(
            method = "setupTerrain",
            slice = @Slice(to = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ViewFrustum;updateChunkPositions(DD)V")),
            at = @At(value = "FIELD", target = "Lnet/minecraft/entity/Entity;posZ:D", opcode = Opcodes.GETFIELD)
    )
    private double getCameraPosZ(Entity entity) {
        if (entity instanceof ViewCameraEntity) {
            return mc.player.posZ;
        } else {
            return entity.posZ;
        }
    }

    @Redirect(
            method = "setupTerrain",
            slice = @Slice(to = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ViewFrustum;updateChunkPositions(DD)V")),
            at = @At(value = "FIELD", target = "Lnet/minecraft/entity/Entity;chunkCoordX:I", opcode = Opcodes.GETFIELD)
    )
    private int getCameraChunkCoordX(Entity entity) {
        if (entity instanceof ViewCameraEntity) {
            return mc.player.chunkCoordX;
        } else {
            return entity.chunkCoordX;
        }
    }

    @Redirect(
            method = "setupTerrain",
            slice = @Slice(to = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ViewFrustum;updateChunkPositions(DD)V")),
            at = @At(value = "FIELD", target = "Lnet/minecraft/entity/Entity;chunkCoordY:I", opcode = Opcodes.GETFIELD)
    )
    private int getCameraChunkCoordY(Entity entity) {
        if (entity instanceof ViewCameraEntity) {
            return mc.player.chunkCoordY;
        } else {
            return entity.chunkCoordY;
        }
    }

    @Redirect(
            method = "setupTerrain",
            slice = @Slice(to = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ViewFrustum;updateChunkPositions(DD)V")),
            at = @At(value = "FIELD", target = "Lnet/minecraft/entity/Entity;chunkCoordZ:I", opcode = Opcodes.GETFIELD)
    )
    private int getCameraChunkCoordZ(Entity entity) {
        if (entity instanceof ViewCameraEntity) {
            return mc.player.chunkCoordZ;
        } else {
            return entity.chunkCoordZ;
        }
    }
}
