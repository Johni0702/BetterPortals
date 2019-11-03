//#if MC>=11400
//$$ package de.johni0702.minecraft.view.impl.mixin;
//$$
//$$ import net.minecraft.client.multiplayer.ClientChunkProvider;
//$$ import net.minecraft.client.world.ClientWorld;
//$$ import net.minecraft.nbt.CompoundNBT;
//$$ import net.minecraft.network.PacketBuffer;
//$$ import net.minecraft.util.math.ChunkPos;
//$$ import net.minecraft.util.math.SectionPos;
//$$ import net.minecraft.world.World;
//$$ import net.minecraft.world.biome.Biome;
//$$ import net.minecraft.world.chunk.Chunk;
//$$ import net.minecraft.world.chunk.ChunkSection;
//$$ import net.minecraft.world.chunk.ChunkStatus;
//$$ import net.minecraft.world.lighting.WorldLightManager;
//$$ import org.apache.logging.log4j.Logger;
//$$ import org.spongepowered.asm.mixin.Final;
//$$ import org.spongepowered.asm.mixin.Mixin;
//$$ import org.spongepowered.asm.mixin.Overwrite;
//$$ import org.spongepowered.asm.mixin.Shadow;
//$$
//$$ import javax.annotation.Nullable;
//$$ import java.util.Map;
//$$ import java.util.concurrent.ConcurrentHashMap;
//$$
//$$ // Replaces the fixed-size chunk array with a dynamically-sized hash-map.
//$$ // Care must be taken that getChunk be thread-safe!
//$$ // TODO Could probably be replaced with injects instead of overwrites but let's wait for CC first
//$$ @SuppressWarnings("OverwriteAuthorRequired")
//$$ @Mixin(ClientChunkProvider.class)
//$$ public abstract class MixinClientChunkProvider {
//$$     @Shadow @Final private WorldLightManager lightManager;
//$$     @Shadow @Final private Chunk empty;
//$$     @Shadow @Final private ClientWorld world;
//$$     @Shadow @Final private static Logger LOGGER;
//$$
//$$     private final Map<ChunkPos, Chunk> chunkMap = new ConcurrentHashMap<>();
//$$
//$$     @Overwrite
//$$     public void unloadChunk(int x, int z) {
//$$         Chunk chunk = chunkMap.remove(new ChunkPos(x, z));
//$$         if (chunk != null) {
//$$             this.world.onChunkUnloaded(chunk);
//$$         }
//$$     }
//$$
//$$     @Overwrite
//$$     @Nullable
//$$     public Chunk getChunk(int chunkX, int chunkZ, ChunkStatus requiredStatus, boolean load) {
//$$         Chunk chunk = chunkMap.get(new ChunkPos(chunkX, chunkZ));
//$$         return chunk != null || !load ? chunk : this.empty;
//$$     }
//$$
//$$     @Overwrite
//$$     @Nullable
//$$     public Chunk updateChunkFromPacket(World worldIn, int x, int z, PacketBuffer buffer, CompoundNBT heightmapTagsNbt, int availableSections, boolean isFullChunk) {
//$$         ChunkPos chunkPos = new ChunkPos(x, z);
//$$         Chunk chunk = chunkMap.get(chunkPos);
//$$         if (chunk == null) {
//$$             if (!isFullChunk) {
//$$                 LOGGER.warn("Ignoring chunk since we don't have complete data: {}, {}", x, z);
//$$                 return null;
//$$             }
//$$
//$$             chunk = new Chunk(worldIn, new ChunkPos(x, z), new Biome[256]);
//$$             chunk.read(buffer, heightmapTagsNbt, availableSections, isFullChunk);
//$$             chunkMap.put(chunkPos, chunk);
//$$         } else {
//$$             chunk.read(buffer, heightmapTagsNbt, availableSections, isFullChunk);
//$$         }
//$$
//$$         ChunkSection[] sections = chunk.getSections();
//$$         this.lightManager.func_215571_a(new ChunkPos(x, z), true);
//$$
//$$         for (int y = 0; y < sections.length; ++y) {
//$$             ChunkSection chunksection = sections[y];
//$$             this.lightManager.updateSectionStatus(SectionPos.of(x, y, z), ChunkSection.isEmpty(chunksection));
//$$         }
//$$
//$$         return chunk;
//$$     }
//$$
//$$     @Overwrite
//$$     public void setCenter(int x, int z) {
//$$         // No-op, we're already dynamically-size
//$$     }
//$$
//$$     @Overwrite
//$$     public void setViewDistance(int viewDistance) {
//$$         // No-op, we're already dynamically-size
//$$     }
//$$
//$$     @Overwrite
//$$     // FIXME preprocessor should handle this?
    //#if FABRIC>=1
    //$$ public String getStatus() {
    //#else
    //$$ public String makeString() {
    //#endif
//$$         return "Client Chunk Cache: " + this.getLoadedChunksCount();
//$$     }
//$$
//$$     @Overwrite
//$$     public int getLoadedChunksCount() {
//$$         return this.chunkMap.size();
//$$     }
//$$ }
//#endif
