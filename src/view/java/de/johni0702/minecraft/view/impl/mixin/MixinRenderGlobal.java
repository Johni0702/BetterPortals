package de.johni0702.minecraft.view.impl.mixin;

import de.johni0702.minecraft.view.impl.client.render.ViewCameraEntity;
import de.johni0702.minecraft.view.impl.client.render.ViewChunkRenderDispatcher;
import de.johni0702.minecraft.view.impl.client.render.ViewRenderManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.entity.Entity;
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
