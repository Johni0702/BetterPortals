package de.johni0702.minecraft.view.impl.mixin;

import de.johni0702.minecraft.view.client.render.ChunkVisibilityDetail;
import de.johni0702.minecraft.view.client.render.RenderPass;
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
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.lib.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static de.johni0702.minecraft.betterportals.common.ExtensionsKt.approxEquals;

//#if MC>=11400
//$$ import net.minecraft.client.entity.player.ClientPlayerEntity;
//$$ import net.minecraft.client.renderer.ActiveRenderInfo;
//#else
//#endif

@Mixin(RenderGlobal.class)
@SideOnly(Side.CLIENT)
public abstract class MixinRenderGlobal {
    @Shadow @Final private Minecraft mc;

    @Shadow public abstract void loadRenderers();

    @Redirect(method = "loadRenderers", at = @At(value = "NEW", target = "net/minecraft/client/renderer/chunk/ChunkRenderDispatcher"))
    private ChunkRenderDispatcher createChunkRenderDispatcher(
            //#if MC>=11400
            //$$ boolean is64bit
            //#endif
    ) {
        return new ViewChunkRenderDispatcher(
                //#if MC>=11400
                //$$ is64bit
                //#endif
        );
    }

    //#if MC>=11400
    //$$ //
    //$$ // MC centers the view frustum on the player as opposed to the view entity.
    //$$ // Didn't do so in 1.12, not sure why it has changed, probably because in vanilla the server sends chunks around
    //$$ // the player, so having it as the center for the view frustum makes sense (did they not think of people who
    //$$ // spectate and have a lower view distance than the server?! or does the player finally move with the spectated
    //$$ // entity, in which case: why this change in the first place?).
    //$$ // What makes this even more stupid is the fact that MC itself still uses the view entity to initialize the
    //$$ // ViewFrustum in `loadRenderer()`...
    //$$ // Anyhow, it's no good for us since our view frustum can easily be repositioned (and must be centered on the
    //$$ // view entity for portals to work properly!), so we undo that change by redirecting any access the mc.player back
    //$$ // to the view entity.
    //$$ //
    //$$ @Redirect(
    //$$         method = "setupTerrain",
    //$$         slice = @Slice(to = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ViewFrustum;updateChunkPositions(DD)V")),
    //$$         at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;player:Lnet/minecraft/client/entity/player/ClientPlayerEntity;", opcode = Opcodes.GETFIELD)
    //$$ )
    //$$ private ClientPlayerEntity centerViewFrustumOnViewEntityInsteadOfPlayer(Minecraft minecraft) {
    //$$     Entity viewEntity = minecraft.getRenderViewEntity();
    //$$     if (viewEntity instanceof ClientPlayerEntity) {
    //$$         return (ClientPlayerEntity) viewEntity;
    //$$     } else {
    //$$         // View entity isn't a player (so not one of our camera entities), let whoever set it deal with this issue
    //$$         // (chances are we're close to the real player anyway).
    //$$         return mc.player;
    //$$     }
    //$$ }
    //#endif


    // See [ChunkVisibilityDetail]
    //#if MC>=11400
    //$$ @Redirect(method = "setupTerrain", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ActiveRenderInfo;getBlockPos()Lnet/minecraft/util/math/BlockPos;"))
    //#else
    @Redirect(method = "setupTerrain", at = @At(value = "NEW", target = "net/minecraft/util/math/BlockPos", ordinal = 0))
    //#endif
    private BlockPos getChunkVisibilityFloodFillOrigin(
            //#if MC>=11400
            //$$ ActiveRenderInfo activeRenderInfo
            //#else
            double orgX, double orgY, double orgZ
            //#endif
    ) {
        RenderPass current = ViewRenderManager.Companion.getINSTANCE().getCurrent();
        if (current != null) {
            BlockPos origin = current.get(ChunkVisibilityDetail.class).getOrigin();
            if (origin != null) {
                return origin;
            }
        }
        //#if MC>=11400
        //$$ return activeRenderInfo.getBlockPos();
        //#else
        return new BlockPos(orgX, orgY, orgZ);
        //#endif
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
    private void refreshIfOtherViewRefreshed(
            //#if MC>=11400
            //$$ ActiveRenderInfo activeRenderInfo,
            //#else
            Entity viewEntity,
            double partialTicks,
            //#endif
            ICamera camera,
            int frameCount,
            boolean playerSpectator,
            CallbackInfo ci
    ) {
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
    // MC re-sorts transparent quads when the view entity has moved at least 1m.
    // This is problematic with views because it happens twice every frame the moment the same world is rendered
    // twice for a single frame for whatever reason (e.g. portals). It'll generate far more chunk upload tasks
    // than can be consumed in the same time and effectively flooding the upload queue and delaying indefinitely any
    // legitimate uploads.
    // To work around the issue, we re-sort whenever the player entity has moved at least 1m. That'll produce incorrect
    // results in other worlds but is far better than breaking chunk uploads.
    // Transparency with multiple portals needs an elaborate solution and solving this problem will be part of that
    // anyway.
    // Additionally, we must only sort when rendering the main (not necessarily the root) view because it'll re-sort
    // only the 15 closest rendered chunks (which have a transparent blocks layer). If we don't, then we'll inevitably
    // sort in one of the child views and sort its chunks, not the one closest to the player.
    //

    @Shadow private double prevRenderSortX;
    @Shadow private double prevRenderSortY;
    @Shadow private double prevRenderSortZ;

    private boolean isMainView() {
        RenderPass pass = ViewRenderManager.Companion.getINSTANCE().getCurrent();
        if (pass != null) {
            Vec3d playerEyePos = mc.player.getPositionEyes(mc.getRenderPartialTicks());
            Vec3d cameraEyePos = pass.getCamera().getEyePosition();
            return approxEquals(cameraEyePos, playerEyePos, 1e-4);
        }
        return false;
    }

    //#if MC>=11400
    //$$ @Redirect(
    //$$         method = "renderBlockLayer(Lnet/minecraft/util/BlockRenderLayer;Lnet/minecraft/client/renderer/ActiveRenderInfo;)I",
    //$$         at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ActiveRenderInfo;getProjectedView()Lnet/minecraft/util/math/Vec3d;")
    //$$ )
    //$$ private Vec3d getPosForTransparencySort(ActiveRenderInfo activeRenderInfo) {
    //$$     return isMainView() ? activeRenderInfo.getProjectedView() : new Vec3d(prevRenderSortX, prevRenderSortY, prevRenderSortZ);
    //$$ }
    //#else
    @Redirect(
            method = "renderBlockLayer(Lnet/minecraft/util/BlockRenderLayer;DILnet/minecraft/entity/Entity;)I",
            at = @At(value = "FIELD", opcode = Opcodes.GETFIELD, target = "Lnet/minecraft/entity/Entity;posX:D")
    )
    private double getPlayerXForTransparencySort(Entity entity) {
        return isMainView() ? mc.player.posX : prevRenderSortX;
    }

    @Redirect(
            method = "renderBlockLayer(Lnet/minecraft/util/BlockRenderLayer;DILnet/minecraft/entity/Entity;)I",
            at = @At(value = "FIELD", opcode = Opcodes.GETFIELD, target = "Lnet/minecraft/entity/Entity;posY:D")
    )
    private double getPlayerYForTransparencySort(Entity entity) {
        return isMainView() ? mc.player.posY : prevRenderSortY;
    }

    @Redirect(
            method = "renderBlockLayer(Lnet/minecraft/util/BlockRenderLayer;DILnet/minecraft/entity/Entity;)I",
            at = @At(value = "FIELD", opcode = Opcodes.GETFIELD, target = "Lnet/minecraft/entity/Entity;posZ:D")
    )
    private double getPlayerZForTransparencySort(Entity entity) {
        return isMainView() ? mc.player.posZ : prevRenderSortZ;
    }
    //#endif
}
