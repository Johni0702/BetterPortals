//#if MC>=11400
//$$ package de.johni0702.minecraft.betterportals.impl.mixin;
//$$
//$$ import de.johni0702.minecraft.betterportals.impl.TheImpl;
//$$ import net.minecraft.entity.Entity;
//$$ import net.minecraft.util.math.RayTraceContext;
//$$ import net.minecraft.util.math.shapes.ISelectionContext;
//$$ import org.spongepowered.asm.mixin.Final;
//$$ import org.spongepowered.asm.mixin.Mixin;
//$$ import org.spongepowered.asm.mixin.Mutable;
//$$ import org.spongepowered.asm.mixin.Shadow;
//$$ import org.spongepowered.asm.mixin.injection.At;
//$$ import org.spongepowered.asm.mixin.injection.Redirect;
//$$
//$$ @Mixin(RayTraceContext.class)
//$$ public abstract class MixinRayTraceContext {
//$$     @Shadow @Final @Mutable private RayTraceContext.BlockMode blockMode;
//$$
//$$     @Shadow @Final @Mutable private RayTraceContext.FluidMode fluidMode;
//$$
//$$     @Shadow @Final @Mutable private ISelectionContext context;
//$$
//$$     @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/shapes/ISelectionContext;forEntity(Lnet/minecraft/entity/Entity;)Lnet/minecraft/util/math/shapes/ISelectionContext;"))
//$$     private ISelectionContext getSelectionContext(Entity entity) {
//$$         MixinRayTraceContext overwrite = (MixinRayTraceContext) (Object) TheImpl.INSTANCE.getRayTraceContextOverwrite();
//$$         if (overwrite != null) {
//$$             this.blockMode = overwrite.blockMode;
//$$             this.fluidMode = overwrite.fluidMode;
//$$             return overwrite.context;
//$$         } else {
//$$             return ISelectionContext.forEntity(entity);
//$$         }
//$$     }
//$$ }
//#endif
