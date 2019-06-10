package de.johni0702.minecraft.view.impl.mixin;

import de.johni0702.minecraft.view.impl.client.render.ViewRenderPlan;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ActiveRenderInfo.class)
public abstract class MixinActiveRenderInfo {
    // See also MixinEntityRenderer#disableFogInView
    @Inject(method = "getBlockStateAtEntityViewpoint", at = @At("HEAD"), cancellable = true)
    private static void disableFogInView(World worldIn, Entity entityIn, float partialTicks, CallbackInfoReturnable<IBlockState> ci) {
        // If we aren't currently rendering the outermost view,
        // then the camera shouldn't ever be considered to be in any blocks
        if (ViewRenderPlan.Companion.getCURRENT() != ViewRenderPlan.Companion.getMAIN()) {
            ci.setReturnValue(Blocks.AIR.getDefaultState());
        }
    }
}
