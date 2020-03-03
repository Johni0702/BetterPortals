//#if MC>=11400
//$$ package de.johni0702.minecraft.betterportals.impl.mixin;
//$$
//$$ import de.johni0702.minecraft.betterportals.impl.IHasMainThread;
//$$ import net.minecraft.world.server.ServerChunkProvider;
//$$ import org.spongepowered.asm.mixin.Final;
//$$ import org.spongepowered.asm.mixin.Mixin;
//$$ import org.spongepowered.asm.mixin.Mutable;
//$$ import org.spongepowered.asm.mixin.Shadow;
//$$
//$$ @Mixin(ServerChunkProvider.class)
//$$ public abstract class Mixin_ServerChunkManager_IHasMainThread implements IHasMainThread {
//$$     @Shadow @Mutable private @Final Thread mainThread;
//$$
//$$     @Override
//$$     public void setMainThread() {
//$$         this.mainThread = Thread.currentThread();
//$$     }
//$$ }
//#endif
