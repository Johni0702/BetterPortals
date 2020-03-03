//#if MC>=11400
//$$ package de.johni0702.minecraft.betterportals.impl.mixin;
//$$
//$$ import de.johni0702.minecraft.betterportals.common.ExtensionsKt;
//$$ import de.johni0702.minecraft.betterportals.impl.common.PortalManagerImpl;
//$$ import net.minecraft.util.math.shapes.ISelectionContext;
//$$ import net.minecraft.util.math.AxisAlignedBB;
//$$ import net.minecraft.util.Direction;
//$$ import net.minecraft.util.math.Vec3d;
//$$ import net.minecraft.util.math.shapes.VoxelShape;
//$$ import net.minecraft.util.math.shapes.VoxelShapes;
//$$ import net.minecraft.world.IWorldReader;
//$$ import net.minecraft.world.World;
//$$ import org.spongepowered.asm.mixin.Mixin;
//$$ import org.spongepowered.asm.mixin.injection.At;
//$$ import org.spongepowered.asm.mixin.injection.Inject;
//$$ import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//$$
//$$ import java.util.stream.Stream;
//$$
//$$ @Mixin(VoxelShapes.class)
//$$ public abstract class Mixin_ModifyFastCollisionsWithBlocks {
//$$     @Inject(method = "getAllowedOffset(Lnet/minecraft/util/Direction$Axis;Lnet/minecraft/util/math/AxisAlignedBB;Lnet/minecraft/world/IWorldReader;DLnet/minecraft/util/math/shapes/ISelectionContext;Ljava/util/stream/Stream;)D", at = @At("HEAD"), cancellable = true)
//$$     private static void calculateOffsetWithPortals(Direction.Axis axis, AxisAlignedBB box, IWorldReader viewableWorld, double maxOffset, ISelectionContext context, Stream<VoxelShape> additionalShapes, CallbackInfoReturnable<Double> ci) {
//$$         if (!(viewableWorld instanceof World)) {
//$$             return;
//$$         }
//$$         World world = (World) viewableWorld;
//$$         AxisAlignedBB query = box.expand(ExtensionsKt.with(Vec3d.ZERO, axis, maxOffset));
//$$         // If the query box is intersecting any portal, we need to go the slow route
//$$         if (PortalManagerImpl.EventHandler.INSTANCE.needsCollisionsModified(world, query)) {
//$$             // No way to determine the entity at this point. For vanilla we get it in Mixin_CaptureCollisionEntity.
//$$             Stream<VoxelShape> blockShapes = viewableWorld.getCollisionShapes(null, query);
//$$             for (VoxelShape shape : (Iterable<VoxelShape>) Stream.concat(blockShapes, additionalShapes)::iterator) {
//$$                 maxOffset = shape.getAllowedOffset(axis, box, maxOffset);
//$$             }
//$$             ci.setReturnValue(maxOffset);
//$$         }
//$$     }
//$$ }
//#endif
