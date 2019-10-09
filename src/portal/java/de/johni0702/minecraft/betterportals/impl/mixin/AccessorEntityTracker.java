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
//$$ @Mixin(targets = "net.minecraft.world.server.ChunkManager$EntityTracker")
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
