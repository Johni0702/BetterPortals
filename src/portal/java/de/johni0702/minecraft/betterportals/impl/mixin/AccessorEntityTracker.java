//#if MC>=11400
//$$ package de.johni0702.minecraft.betterportals.impl.mixin;
//$$
//$$ import net.minecraft.entity.player.ServerPlayerEntity;
//$$ import net.minecraft.util.math.SectionPos;
//$$ import net.minecraft.world.TrackedEntity;
//$$ import org.spongepowered.asm.mixin.Mixin;
//$$ import org.spongepowered.asm.mixin.gen.Accessor;
//$$
//$$ import java.util.Set;
//$$
//$$ // FIXME preprocessor should also be able to handle package.Outer.Inner format (bug in fabric's AP requires it)
//#if FABRIC>=1
//$$ @Mixin(targets = "net.minecraft.server.world.ThreadedAnvilChunkStorage.EntityTracker")
//#else
//$$ @Mixin(targets = "net.minecraft.world.server.ChunkManager$EntityTracker")
//#endif
//$$ public interface AccessorEntityTracker {
//$$     @Accessor
//$$     Set<ServerPlayerEntity> getTrackingPlayers();
//$$     @Accessor
//$$     TrackedEntity getEntry();
//$$     @Accessor
//$$     SectionPos getPos();
//$$     @Accessor
//$$     void setPos(SectionPos pos);
//$$ }
//#endif
