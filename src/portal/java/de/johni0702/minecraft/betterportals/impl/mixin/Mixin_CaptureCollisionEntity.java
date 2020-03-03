//#if MC>=11400
//$$ package de.johni0702.minecraft.betterportals.impl.mixin;
//$$
//$$ import de.johni0702.minecraft.betterportals.impl.common.PortalManagerImpl;
//$$ import net.minecraft.entity.Entity;
//$$ import net.minecraft.util.math.shapes.ISelectionContext;
//$$ import net.minecraft.util.ReuseableStream;
//$$ import net.minecraft.util.math.AxisAlignedBB;
//$$ import net.minecraft.util.math.Vec3d;
//$$ import net.minecraft.util.math.shapes.VoxelShape;
//$$ import net.minecraft.world.World;
//$$ import org.spongepowered.asm.mixin.Mixin;
//$$ import org.spongepowered.asm.mixin.injection.At;
//$$ import org.spongepowered.asm.mixin.injection.Inject;
//$$ import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//$$
//$$ import javax.annotation.Nullable;
//$$ import java.util.ArrayDeque;
//$$ import java.util.Deque;
//$$
//$$ @Mixin(Entity.class)
//$$ public abstract class Mixin_CaptureCollisionEntity {
//$$     private static ThreadLocal<Deque<PortalManagerImpl.EventHandler.CollisionContextHandle>> calculateMotionVectorContextStack = ThreadLocal.withInitial(ArrayDeque::new);
//$$
//$$     @Inject(method = "func_223307_a", at = @At("HEAD"))
//$$     private static void enterCollisionContext(@Nullable Entity entity, Vec3d vec3d_1, AxisAlignedBB box_1, World world_1, ISelectionContext entityContext_1, ReuseableStream<VoxelShape> reusableStream_1, CallbackInfoReturnable<Double> ci) {
//$$         if (entity != null) {
//$$             calculateMotionVectorContextStack.get().push(
//$$                     PortalManagerImpl.EventHandler.INSTANCE.enterCollisionContext(entity));
//$$         }
//$$     }
//$$
//$$     @Inject(method = "func_223307_a", at = @At("RETURN"))
//$$     private static void leaveCollisionContext(@Nullable Entity entity, Vec3d vec3d_1, AxisAlignedBB box_1, World world_1, ISelectionContext entityContext_1, ReuseableStream<VoxelShape> reusableStream_1, CallbackInfoReturnable<Double> ci) {
//$$         if (entity != null) {
//$$             PortalManagerImpl.EventHandler.INSTANCE.leaveCollisionContext(
//$$                     calculateMotionVectorContextStack.get().pop());
//$$         }
//$$     }
//$$ }
//#endif
