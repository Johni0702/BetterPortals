//#if MC>=11400
//$$ package de.johni0702.minecraft.betterportals.impl.mixin;
//$$
//$$ import com.google.common.util.concurrent.MoreExecutors;
//$$ import net.minecraft.util.Util;
//$$ import org.spongepowered.asm.mixin.Mixin;
//$$ import org.spongepowered.asm.mixin.Overwrite;
//$$
//$$ import java.util.concurrent.ExecutorService;
//$$
//$$ @Mixin(Util.class)
//$$ public abstract class Mixin_DirectServerWorkerExecutor {
//$$     /**
//$$      * @reason Use a direct executor regardless of CPU threads so we get deterministic behavior during tests.
//$$      * @author johni0702
//$$      */
//$$     @Overwrite
//$$     private static ExecutorService createServerExecutor() {
//$$         return MoreExecutors.newDirectExecutorService();
//$$     }
//$$ }
//#endif
