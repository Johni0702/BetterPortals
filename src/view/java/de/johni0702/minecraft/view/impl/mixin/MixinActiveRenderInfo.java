package de.johni0702.minecraft.view.impl.mixin;

import de.johni0702.minecraft.view.impl.client.render.ViewRenderPlan;
import net.minecraft.client.renderer.ActiveRenderInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

//#if FABRIC>=1
//#else
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
//#endif

//#if MC>=11400
//$$ import net.minecraft.fluid.Fluids;
//$$ import net.minecraft.fluid.IFluidState;
//#else
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
//#endif

@Mixin(ActiveRenderInfo.class)
public abstract class MixinActiveRenderInfo {
    // See also Mixin_BlockBasedFogColor
    //#if MC>=11400
    //#if FABRIC>=1
    //$$ // FIXME port to fabric
    //#else
    //$$ @Inject(method = "getBlockAtCamera", at = @At("HEAD"), cancellable = true, remap = false)
    //$$ private void ignoreBlockInView(CallbackInfoReturnable<BlockState> ci) {
    //$$     // If we aren't currently rendering the outermost view,
    //$$     // then the camera shouldn't ever be considered to be in any blocks
    //$$     if (ViewRenderPlan.Companion.getCURRENT() != ViewRenderPlan.Companion.getMAIN()) {
    //$$         ci.setReturnValue(Blocks.AIR.getDefaultState());
    //$$     }
    //$$ }
    //#endif
    //$$
    //$$ @Inject(method = "getFluidState", at = @At("HEAD"), cancellable = true)
    //$$ private void ignoreFluidInView(CallbackInfoReturnable<IFluidState> ci) {
    //$$     // If we aren't currently rendering the outermost view,
    //$$     // then the camera shouldn't ever be considered to be in any fluids
    //$$     if (ViewRenderPlan.Companion.getCURRENT() != ViewRenderPlan.Companion.getMAIN()) {
    //$$         ci.setReturnValue(Fluids.EMPTY.getDefaultState());
    //$$     }
    //$$ }
    //#else
    @Inject(method = "getBlockStateAtEntityViewpoint", at = @At("HEAD"), cancellable = true)
    private static void disableFogInView(World worldIn, Entity entityIn, float partialTicks, CallbackInfoReturnable<IBlockState> ci) {
        // If we aren't currently rendering the outermost view,
        // then the camera shouldn't ever be considered to be in any blocks
        if (ViewRenderPlan.Companion.getCURRENT() != ViewRenderPlan.Companion.getMAIN()) {
            ci.setReturnValue(Blocks.AIR.getDefaultState());
        }
    }
    //#endif
}
