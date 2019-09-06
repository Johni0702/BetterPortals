//#if MC<11400
package de.johni0702.minecraft.view.impl.mixin;

import de.johni0702.minecraft.view.impl.server.PlayerCubeMapHandler;
import de.johni0702.minecraft.view.impl.server.ServerWorldManager;
import de.johni0702.minecraft.view.impl.server.ServerWorldManagerKt;
import de.johni0702.minecraft.view.impl.server.ServerWorldsManagerImpl;
import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.core.CubicChunks;
import io.github.opencubicchunks.cubicchunks.core.server.ChunkGc;
import io.github.opencubicchunks.cubicchunks.core.server.CubeWatcher;
import io.github.opencubicchunks.cubicchunks.core.server.PlayerCubeMap;
import io.github.opencubicchunks.cubicchunks.core.visibility.CubeSelector;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.management.PlayerChunkMap;
import net.minecraft.server.management.PlayerChunkMapEntry;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.WorldServer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nonnull;
import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import static de.johni0702.minecraft.view.impl.ViewAPIImplKt.getWorldsManagerImpl;

@Mixin(PlayerCubeMap.class)
public abstract class MixinPlayerCubeMap_CC extends PlayerChunkMap {
    @Shadow(remap = false) protected abstract CubeWatcher getOrCreateCubeWatcher(@Nonnull CubePos cubePos);

    @Shadow(remap = false) protected abstract void setNeedSort();

    @Shadow(remap = false) @Final private ChunkGc chunkGc;

    @Shadow(remap = false) private int horizontalViewDistance;

    @Shadow(remap = false) private int verticalViewDistance;

    // Converting Vec3i to CubePos
    private CubeWatcher getOrCreateCubeWatcher(Vec3i cubePos) {
        return getOrCreateCubeWatcher(new CubePos(cubePos.getX(), cubePos.getY(), cubePos.getZ()));
    }

    // Workaround for https://github.com/SpongePowered/Mixin/issues/284
    private Function<ChunkPos, PlayerChunkMapEntry> getOrCreateColumnWatcherFunc;
    private PlayerChunkMapEntry invokeGetOrCreateColumnWatcher(ChunkPos chunkPos) {
        if (getOrCreateColumnWatcherFunc == null) {
            try {
                MethodHandles.Lookup lookup = MethodHandles.lookup();
                MethodHandle handle = lookup.unreflect(getClass().getDeclaredMethod("getOrCreateColumnWatcher", ChunkPos.class));
                //noinspection unchecked
                getOrCreateColumnWatcherFunc = (Function<ChunkPos, PlayerChunkMapEntry>) LambdaMetafactory.metafactory(
                        lookup,
                        "apply",
                        MethodType.methodType(Function.class, getClass()),
                        MethodType.methodType(Object.class, Object.class),
                        handle,
                        MethodType.methodType(PlayerChunkMapEntry.class, ChunkPos.class)
                ).getTarget().invoke(this);
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }
        return getOrCreateColumnWatcherFunc.apply(chunkPos);
    }

    public MixinPlayerCubeMap_CC(WorldServer serverWorld) { super(serverWorld); }

    @Redirect(method = "addPlayer", at = @At(value = "INVOKE", target = "Ljava/util/List;contains(Ljava/lang/Object;)Z", remap = false))
    private boolean forceAddDuringSwap(List list, Object o) {
        if (PlayerCubeMapHandler.INSTANCE.getSwapInProgress()) {
            return true;
        } else {
            return list.contains(o);
        }
    }

    private EntityPlayerMP player;
    @Inject(method = {"addPlayer", "removePlayer", "updateMovingPlayer"}, at = @At("HEAD"))
    private void recordPlayerArgument(EntityPlayerMP player, CallbackInfo ci) {
        this.player = player;
    }

    @Redirect(method = "addPlayer", at = @At(value = "INVOKE", target = "Lio/github/opencubicchunks/cubicchunks/core/visibility/CubeSelector;forAllVisibleFrom(Lio/github/opencubicchunks/cubicchunks/api/util/CubePos;IILjava/util/function/Consumer;)V", remap = false))
    private void addPlayerWithViews(CubeSelector self, CubePos playerPos, int horizontalViewDist, int verticalViewDist, Consumer<CubePos> func) {
        ServerWorldsManagerImpl worldsManager = getWorldsManagerImpl(player);
        ServerWorldManager worldManager = worldsManager.getWorldManagers().get(getWorldServer());
        if (PlayerCubeMapHandler.INSTANCE.getSwapInProgress()) {
            for (ChunkPos columnPos : worldManager.getTrackedColumns()) {
                PlayerChunkMapEntry watcher = invokeGetOrCreateColumnWatcher(columnPos);
                watcher.addPlayer(player);
            }
            for (Vec3i cubePos : worldManager.getTrackedCubes()) {
                CubeWatcher watcher = getOrCreateCubeWatcher(cubePos);
                ServerWorldManagerKt.addPlayer(watcher, player);
            }
        } else {
            worldsManager.updateActiveViews();
            worldManager.updateTrackedColumnsAndCubes(this::invokeGetOrCreateColumnWatcher, this::getOrCreateCubeWatcher);
        }
    }

    @Redirect(method = "removePlayer", at = @At(value = "INVOKE", target = "Lio/github/opencubicchunks/cubicchunks/core/visibility/CubeSelector;forAllVisibleFrom(Lio/github/opencubicchunks/cubicchunks/api/util/CubePos;IILjava/util/function/Consumer;)V", remap = false))
    private void removePlayerWithViews(CubeSelector self, CubePos playerPos, int horizontalViewDist, int verticalViewDist, Consumer<CubePos> func) {
        ServerWorldsManagerImpl worldsManager = getWorldsManagerImpl(player);
        ServerWorldManager worldManager = worldsManager.getWorldManagers().get(getWorldServer());
        for (Vec3i cubePos : worldManager.getTrackedCubes()) {
            CubeWatcher watcher = getOrCreateCubeWatcher(cubePos);
            ServerWorldManagerKt.removePlayer(watcher, player);
        }
        worldManager.getTrackedCubes().clear();
        for (ChunkPos columnPos : worldManager.getTrackedColumns()) {
            PlayerChunkMapEntry watcher = invokeGetOrCreateColumnWatcher(columnPos);
            watcher.removePlayer(player);
        }
        worldManager.getTrackedColumns().clear();
    }

    @Inject(method = "updatePlayer", at = @At("HEAD"), cancellable = true, remap = false)
    private void updateActiveViews(@Coerce Object entry, CubePos oldPos, CubePos newPos, CallbackInfo ci) {
        getWorldsManagerImpl(player).updateActiveViews();
        ci.cancel();
    }

    @Inject(method = "updateMovingPlayer", at = @At("RETURN"))
    private void updateTrackedColumnsAndCubes(EntityPlayerMP player, CallbackInfo ci) {
        ServerWorldManager worldManager = getWorldsManagerImpl(player).getWorldManagers().get(getWorldServer());
        if (worldManager.getNeedsUpdate()) {
            worldManager.updateTrackedColumnsAndCubes(this::invokeGetOrCreateColumnWatcher, this::getOrCreateCubeWatcher);
            worldManager.setNeedsUpdate(false);
            this.setNeedSort();
            this.chunkGc.tick();
        }
    }

    @Inject(method = "setPlayerViewDistance", at = @At("HEAD"), cancellable = true, remap = false)
    private void updateTrackedOnViewDistanceIncrease(int newHorizontalViewDistance, int newVerticalViewDistance, CallbackInfo ci) {
        ci.cancel();

        newHorizontalViewDistance = MathHelper.clamp(newHorizontalViewDistance, 3, CubicChunks.hasOptifine() ? 64 : 32);
        newVerticalViewDistance = MathHelper.clamp(newVerticalViewDistance, 3, CubicChunks.hasOptifine() ? 64 : 32);
        if (this.horizontalViewDistance == newHorizontalViewDistance && this.verticalViewDistance == newVerticalViewDistance) {
            return;
        }
        this.horizontalViewDistance = newHorizontalViewDistance;
        this.verticalViewDistance = newVerticalViewDistance;

        for (EntityPlayer player : new ArrayList<>(getWorldServer().playerEntities)) {
            if (!(player instanceof EntityPlayerMP)) continue;

            ServerWorldsManagerImpl worldsManager = getWorldsManagerImpl((EntityPlayerMP) player);
            if (worldsManager == null) continue;
            worldsManager.updateActiveViews();

            ServerWorldManager worldManager = worldsManager.getWorldManagers().get(getWorldServer());
            if (worldManager == null) continue;
            worldManager.updateTrackedColumnsAndCubes(this::invokeGetOrCreateColumnWatcher, this::getOrCreateCubeWatcher);
        }
    }
}
//#endif
