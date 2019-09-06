//#if MC>=11400
//$$ package de.johni0702.minecraft.view.impl.mixin;
//$$
//$$ import de.johni0702.minecraft.betterportals.impl.IThreadTaskExecutor;
//$$ import de.johni0702.minecraft.view.impl.client.ViewDemuxingTaskQueue;
//$$ import net.minecraft.util.concurrent.ThreadTaskExecutor;
//$$ import net.minecraftforge.api.distmarker.Dist;
//$$ import net.minecraftforge.api.distmarker.OnlyIn;
//$$ import org.jetbrains.annotations.NotNull;
//$$ import org.spongepowered.asm.mixin.Final;
//$$ import org.spongepowered.asm.mixin.Mixin;
//$$ import org.spongepowered.asm.mixin.Shadow;
//$$ import org.spongepowered.asm.mixin.injection.At;
//$$ import org.spongepowered.asm.mixin.injection.Inject;
//$$ import org.spongepowered.asm.mixin.injection.ModifyVariable;
//$$ import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//$$ import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//$$
//$$ import java.util.Queue;
//$$ import java.util.concurrent.locks.LockSupport;
//$$
//$$ @Mixin(ThreadTaskExecutor.class)
//$$ @OnlyIn(Dist.CLIENT)
//$$ public abstract class MixinThreadTaskExecutor implements IThreadTaskExecutor<Runnable> {
//$$     @Shadow protected abstract void run(Runnable taskIn);
//$$     @Shadow protected abstract Thread getExecutionThread();
//$$     @Shadow @Final private Queue<Runnable> queue;
//$$     private Queue<Runnable> blockingQueue;
//$$
//$$     @NotNull
//$$     @Override
//$$     public Queue<Runnable> getQueue() {
//$$         return queue;
//$$     }
//$$
//$$     @NotNull
//$$     @Override
//$$     public Queue<Runnable> getBlockingQueue() {
//$$         return blockingQueue;
//$$     }
//$$
//$$     @Override
//$$     public void setBlockingQueue(Queue<Runnable> blockingQueue) {
//$$         this.blockingQueue = blockingQueue;
//$$     }
//$$
//$$     @ModifyVariable(method = "enqueue", at = @At("HEAD"))
//$$     private Runnable wrapWithView(Runnable task) {
//$$         return ViewDemuxingTaskQueue.wrapTask(task);
//$$     }
//$$     @Inject(method = "enqueue", at = @At("HEAD"), cancellable = true)
//$$     private void enqueueTransaction(Runnable task, CallbackInfo ci) {
//$$         Queue<Runnable> blockingQueue = this.blockingQueue;
//$$         if (blockingQueue != null) {
//$$             blockingQueue.add(wrapWithView(task));
//$$             LockSupport.unpark(this.getExecutionThread());
//$$             ci.cancel();
//$$         }
//$$     }
//$$
//$$     @Inject(method = "driveOne", at = @At("HEAD"))
//$$     private void driveTransaction(CallbackInfoReturnable<Boolean> ci) {
//$$         Queue<Runnable> blockingQueue = this.blockingQueue;
//$$         if (blockingQueue == null) return;
//$$
//$$         // Drain the vanilla queue first (to preserve order)
//$$         while (this.queue.peek() != null) {
//$$             run(this.queue.remove());
//$$         }
//$$
//$$         // Drain the main queue (may be blocking)
//$$         while (this.blockingQueue != null) {
//$$             run(blockingQueue.poll());
//$$         }
//$$
//$$         // Drain any items remaining in the (now no longer installed) blocking queue
//$$         while (blockingQueue.peek() != null) {
//$$             run(blockingQueue.remove());
//$$         }
//$$     }
//$$ }
//#endif
