//#if MC>=11400
//$$ package de.johni0702.minecraft.view.impl.mixin;
//$$
//$$ import de.johni0702.minecraft.view.impl.server.PlayerChunkMapHandler;
//$$ import de.johni0702.minecraft.view.impl.server.ServerWorldManager;
//$$ import de.johni0702.minecraft.view.impl.server.ServerWorldsManagerImpl;
//$$ import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
//$$ import net.minecraft.entity.player.ServerPlayerEntity;
//$$ import net.minecraft.network.IPacket;
//$$ import net.minecraft.util.math.ChunkPos;
//$$ import net.minecraft.util.math.MathHelper;
//$$ import net.minecraft.world.server.ChunkHolder;
//$$ import net.minecraft.world.server.ChunkManager;
//$$ import net.minecraft.world.server.ServerWorld;
//$$ import net.minecraft.world.server.TicketManager;
//$$ import org.jetbrains.annotations.NotNull;
//$$ import org.spongepowered.asm.mixin.Final;
//$$ import org.spongepowered.asm.mixin.Mixin;
//$$ import org.spongepowered.asm.mixin.Shadow;
//$$ import org.spongepowered.asm.mixin.injection.At;
//$$ import org.spongepowered.asm.mixin.injection.Inject;
//$$ import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//$$
//$$ import java.lang.invoke.LambdaMetafactory;
//$$ import java.lang.invoke.MethodHandle;
//$$ import java.lang.invoke.MethodHandles;
//$$ import java.lang.invoke.MethodType;
//$$ import java.lang.reflect.Method;
//$$ import java.util.*;
//$$ import java.util.function.Supplier;
//$$ import java.util.stream.Stream;
//$$
//$$ import static de.johni0702.minecraft.view.impl.ViewAPIImplKt.getWorldsManagerImpl;
//$$
//$$ @Mixin(ChunkManager.class)
//$$ public abstract class MixinChunkManager implements PlayerChunkMapHandler.IChunkManager {
//$$
//$$     @Shadow @Final private ServerWorld world;
//$$     @Shadow private int viewDistance;
//$$     @Shadow @Final private Long2ObjectLinkedOpenHashMap<ChunkHolder> field_219251_e;
//$$     @Shadow public abstract Stream<ServerPlayerEntity> getTrackingPlayers(ChunkPos pos, boolean boundaryOnly);
//$$     @Shadow protected abstract void setChunkLoadedAtClient(ServerPlayerEntity player, ChunkPos chunkPosIn, IPacket<?>[] packetCache, boolean wasLoaded, boolean load);
//$$     // Workaround for https://github.com/SpongePowered/Mixin/issues/284
//$$     // @Shadow @Final private ChunkManager.ProxyTicketManager ticketManager;
//$$     private Supplier<TicketManager> getTheTicketManagerFunc;
//$$     private TicketManager getTheTicketManager() {
//$$         if (getTheTicketManagerFunc == null) {
//$$             try {
//$$                 MethodHandles.Lookup lookup = MethodHandles.lookup();
//$$                 Method getTicketManagerMethod;
//$$                 try {
//$$                     getTicketManagerMethod = ChunkManager.class.getDeclaredMethod("getTicketManager");
//$$                 } catch (NoSuchMethodException ignored) {
//$$                     getTicketManagerMethod = ChunkManager.class.getDeclaredMethod("func_219246_e");
//$$                 }
//$$                 MethodHandle handle = lookup.unreflect(getTicketManagerMethod);
//$$                 //noinspection unchecked
//$$                 getTheTicketManagerFunc = (Supplier<TicketManager>) LambdaMetafactory.metafactory(
//$$                         lookup,
//$$                         "get",
//$$                         MethodType.methodType(Supplier.class, ChunkManager.class),
//$$                         MethodType.methodType(Object.class),
//$$                         handle,
//$$                         MethodType.methodType(TicketManager.class)
//$$                 ).getTarget().invoke((ChunkManager) (Object) this);
//$$             } catch (Throwable e) {
//$$                 throw new RuntimeException(e);
//$$             }
//$$         }
//$$         return getTheTicketManagerFunc.get();
//$$     }
//$$
//$$     private Set<ServerPlayerEntity> players = Collections.newSetFromMap(new IdentityHashMap<>());
//$$
//$$     @Override
//$$     public void addPlayerForSwap(@NotNull ServerPlayerEntity player) {
//$$         players.add(player);
//$$     }
//$$
//$$     @Override
//$$     public void removePlayerForSwap(@NotNull ServerPlayerEntity player) {
//$$         players.remove(player);
//$$     }
//$$
//$$     @Inject(method = "setViewDistance", at = @At("HEAD"))
//$$     private void setPlayerViewDistanceWithViews(int viewDistance, CallbackInfo ci) {
//$$         viewDistance = MathHelper.clamp(viewDistance + 1, 3, 33);
//$$         if (viewDistance == this.viewDistance) {
//$$             return;
//$$         }
//$$
//$$         this.viewDistance = viewDistance;
//$$         ((AccessorTicketManager) getTheTicketManager()).invokeSetViewDistance(viewDistance);
//$$
//$$         for (ServerPlayerEntity player : new ArrayList<>(players)) {
//$$             ServerWorldsManagerImpl worldsManager = getWorldsManagerImpl(player);
//$$             worldsManager.updateActiveViews(); // TODO should probably happen before ticket graph update
//$$             ServerWorldManager worldManager = worldsManager.getWorldManagers().get(world);
//$$             worldManager.updateTrackedColumns((pos, load) -> setChunkLoadedAtClient(player, pos, new IPacket[2], !load, load));
//$$         }
//$$     }
//$$
//$$     @Inject(method = "setPlayerTracking", at = @At(value = "NEW", target = "net/minecraft/util/math/ChunkPos"), cancellable = true)
//$$     private void setPlayerTrackingWithViews(ServerPlayerEntity player, boolean track, CallbackInfo ci) {
//$$         ci.cancel();
//$$
//$$         if (!(track ? players.add(player) : players.remove(player))) {
//$$             return;
//$$         }
//$$
//$$         ServerWorldsManagerImpl worldsManager = getWorldsManagerImpl(player);
//$$         ServerWorldManager worldManager = worldsManager.getWorldManagers().get(world);
//$$         if (track) {
//$$             worldsManager.updateActiveViews(); // TODO should probably happen before ticket graph update
//$$             worldManager.updateTrackedColumns((pos, load) -> setChunkLoadedAtClient(player, pos, new IPacket[2], !load, load));
//$$         } else {
//$$             for (ChunkPos pos : worldManager.getTrackedColumns()) {
//$$                 setChunkLoadedAtClient(player, pos, new IPacket[2], true, false);
//$$             }
//$$             worldManager.getTrackedColumns().clear();
//$$         }
//$$     }
//$$
//$$     @Inject(method = "updatePlayerPosition", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/SectionPos;getSectionX()I"), cancellable = true)
//$$     private void updateMovingPlayerWithViews(ServerPlayerEntity player, CallbackInfo ci) {
//$$         ci.cancel();
//$$
//$$         ServerWorldsManagerImpl worldsManager = getWorldsManagerImpl(player);
//$$         ServerWorldManager worldManager = worldsManager.getWorldManagers().get(world);
//$$         worldsManager.updateActiveViews(); // TODO should probably happen before ticket graph update
//$$         if (worldManager.getNeedsUpdate()) {
//$$             worldManager.updateTrackedColumns((pos, load) -> setChunkLoadedAtClient(player, pos, new IPacket[2], !load, load));
//$$             worldManager.setNeedsUpdate(false);
//$$         }
//$$     }
//$$ }
//#endif
