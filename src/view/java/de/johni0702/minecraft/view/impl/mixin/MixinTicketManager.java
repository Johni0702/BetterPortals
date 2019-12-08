//#if MC>=11400
//$$ package de.johni0702.minecraft.view.impl.mixin;
//$$
//$$ import de.johni0702.minecraft.view.impl.server.ServerWorldManager;
//$$ import de.johni0702.minecraft.view.server.CuboidCubeSelector;
//$$ import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
//$$ import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
//$$ import it.unimi.dsi.fastutil.objects.ObjectAVLTreeSet;
//$$ import it.unimi.dsi.fastutil.objects.ObjectSet;
//$$ import it.unimi.dsi.fastutil.objects.ObjectSortedSet;
//$$ import kotlin.Pair;
//$$ import net.minecraft.entity.player.ServerPlayerEntity;
//$$ import net.minecraft.util.math.ChunkPos;
//$$ import net.minecraft.util.math.SectionPos;
//$$ import net.minecraft.world.chunk.ChunkDistanceGraph;
//$$ import net.minecraft.world.server.TicketManager;
//$$ import org.jetbrains.annotations.NotNull;
//$$ import org.spongepowered.asm.mixin.Final;
//$$ import org.spongepowered.asm.mixin.Mixin;
//$$ import org.spongepowered.asm.mixin.Shadow;
//$$ import org.spongepowered.asm.mixin.injection.At;
//$$ import org.spongepowered.asm.mixin.injection.Constant;
//$$ import org.spongepowered.asm.mixin.injection.Inject;
//$$ import org.spongepowered.asm.mixin.injection.ModifyConstant;
//$$ import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//$$
//$$ import java.lang.reflect.Field;
//$$ import java.util.Comparator;
//$$ import java.util.NoSuchElementException;
//$$
//$$ @Mixin(TicketManager.class)
//$$ public abstract class MixinTicketManager implements ServerWorldManager.ITicketManager {
//$$     @Shadow @Final private Long2ObjectMap<ObjectSet<ServerPlayerEntity>> playersByChunkPos;
//$$
//$$     // Workaround for https://github.com/SpongePowered/Mixin/issues/284
//$$     // @Shadow @Final private TicketManager.PlayerChunkTracker playerChunkTracker;
//$$     // @Shadow @Final private TicketManager.PlayerTicketTracker playerTicketTracker;
//$$     private ChunkDistanceGraph _playerChunkTracker;
//$$     private ChunkDistanceGraph _playerTicketTracker;
//$$     private ChunkDistanceGraph getPlayerChunkTracker() {
//$$         if (_playerChunkTracker == null) {
//$$             try {
//$$                 Field field;
//$$                 try {
//$$                     field = TicketManager.class.getDeclaredField(
                            //#if FABRIC>=1
                            //$$ "distanceFromNearestPlayerTracker"
                            //#else
                            //$$ "playerChunkTracker"
                            //#endif
//$$                     );
//$$                 } catch (NoSuchFieldException ignored) {
//$$                     field = TicketManager.class.getDeclaredField(
                            //#if FABRIC>=1
                            //$$ "field_17454"
                            //#else
                            //$$ "field_219381_i"
                            //#endif
//$$                     );
//$$                 }
//$$                 _playerChunkTracker = (ChunkDistanceGraph) field.get(this);
//$$             } catch (Throwable e) {
//$$                 throw new RuntimeException(e);
//$$             }
//$$         }
//$$         return _playerChunkTracker;
//$$     }
//$$     private ChunkDistanceGraph getPlayerTicketTracker() {
//$$         if (_playerTicketTracker == null) {
//$$             try {
//$$                 Field field;
//$$                 try {
//$$                     field = TicketManager.class.getDeclaredField(
                            //#if FABRIC>=1
                            //$$ "nearbyChunkTicketUpdater"
                            //#else
                            //$$ "playerTicketTracker"
                            //#endif
//$$                     );
//$$                 } catch (NoSuchFieldException ignored) {
//$$                     field = TicketManager.class.getDeclaredField(
                            //#if FABRIC>=1
                            //$$ "field_17455"
                            //#else
                            //$$ "field_219382_j"
                            //#endif
//$$                     );
//$$                 }
//$$                 _playerTicketTracker = (ChunkDistanceGraph) field.get(this);
//$$             } catch (Throwable e) {
//$$                 throw new RuntimeException(e);
//$$             }
//$$         }
//$$         return _playerTicketTracker;
//$$     }
//$$
//$$     private final Long2ObjectMap<ObjectSortedSet<Pair<ServerPlayerEntity, CuboidCubeSelector>>> views = new Long2ObjectOpenHashMap<>();
//$$
//$$     @Override
//$$     public void addCuboidView(@NotNull ServerPlayerEntity player, @NotNull CuboidCubeSelector selector) {
//$$         long coord = ChunkPos.asLong(selector.getCenter().getX(), selector.getCenter().getZ());
//$$         views.computeIfAbsent(coord, _coord -> new ObjectAVLTreeSet<>(
//$$                 Comparator.comparing((Pair<ServerPlayerEntity, CuboidCubeSelector> pair) ->
//$$                         pair.component2().getHorizontalEffectiveDistance()
//$$                 ).reversed().thenComparing(pair -> pair.component1().getEntityId())
//$$         )).add(new Pair<>(player, selector));
//$$         int level = getSourceLevelForChunk(coord);
//$$         getPlayerChunkTracker().updateSourceLevel(coord, level, true);
//$$         getPlayerTicketTracker().updateSourceLevel(coord, level, true);
//$$     }
//$$
//$$     @Override
//$$     public void removeCuboidView(@NotNull ServerPlayerEntity player, @NotNull CuboidCubeSelector selector) {
//$$         long coord = ChunkPos.asLong(selector.getCenter().getX(), selector.getCenter().getZ());
//$$         ObjectSortedSet<Pair<ServerPlayerEntity, CuboidCubeSelector>> set = views.get(coord);
//$$         if (!set.remove(new Pair<>(player, selector))) {
//$$             throw new NoSuchElementException(player + " " + selector);
//$$         }
//$$         if (set.isEmpty()) {
//$$             views.remove(coord);
//$$         }
//$$         int level = getSourceLevelForChunk(coord);
//$$         getPlayerChunkTracker().updateSourceLevel(coord, level, false);
//$$         getPlayerTicketTracker().updateSourceLevel(coord, level, false);
//$$     }
//$$
//$$     @Override
//$$     public int getSourceLevelForChunk(long chunkPos) {
//$$         ObjectSet<ServerPlayerEntity> players = playersByChunkPos.get(chunkPos);
//$$         if (players != null && !players.isEmpty()) {
//$$             return 0; // player is right inside this chunk, distance is 0
//$$         }
//$$         ObjectSortedSet<Pair<ServerPlayerEntity, CuboidCubeSelector>> views = this.views.get(chunkPos);
//$$         if (views != null && !views.isEmpty()) {
//$$             int distance = viewDistance - views.first().component2().getHorizontalEffectiveDistance();
//$$             return Math.max(0, distance);
//$$         }
//$$         return Integer.MAX_VALUE;
//$$     }
//$$
//$$     private int viewDistance;
//$$
//$$     @Inject(method = "setViewDistance", at = @At("HEAD"))
//$$     private void recordViewDistance(int viewDistance, CallbackInfo ci) {
//$$         this.viewDistance = viewDistance - 1;
//$$     }
//$$
//$$     private SectionPos pos;
//$$     @Inject(method = "removePlayer", at = @At("HEAD"))
//$$     private void recordPos(SectionPos pos, ServerPlayerEntity player, CallbackInfo ci) {
//$$         this.pos = pos;
//$$     }
//$$
//$$     @ModifyConstant(method = "removePlayer", constant = @Constant(intValue = Integer.MAX_VALUE))
//$$     private int getSourceLevelWithViews(int invalidLevel) {
//$$         return getSourceLevelForChunk(pos.asLong());
//$$     }
//$$ }
//#endif
