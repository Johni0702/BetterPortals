package de.johni0702.minecraft.view.impl.mixin;

import de.johni0702.minecraft.view.impl.client.render.ViewRenderPlan;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

//#if MC>=11400
//$$ import net.minecraft.client.renderer.ActiveRenderInfo;
//$$ import net.minecraft.client.renderer.FogRenderer;
//$$ import net.minecraft.fluid.Fluids;
//$$ import net.minecraft.fluid.IFluidState;
//#else
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.init.Blocks;
//#endif

//#if MC>=11400
//$$ @Mixin(FogRenderer.class)
//#else
@Mixin(EntityRenderer.class)
//#endif
public abstract class Mixin_BlockBasedFogColor {
    // See also MixinActiveRenderInfo#disableFogInView
    @Redirect(
            method = "updateFogColor",
            at = @At(
                    value = "INVOKE",
                    //#if MC>=11400
                    //$$ target = "Lnet/minecraft/client/renderer/ActiveRenderInfo;getFluidState()Lnet/minecraft/fluid/IFluidState;"
                    //#else
                    target = "Lnet/minecraft/client/multiplayer/WorldClient;getBlockState(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/block/state/IBlockState;"
                    //#endif
            )
    )
    //#if MC>=11400
    //$$ private IFluidState disableFogInView(ActiveRenderInfo activeRenderInfo) {
    //#else
    private IBlockState disableFogInView(WorldClient world, BlockPos blockPos) {
    //#endif
        // If we aren't currently rendering the outermost view,
        // then the camera shouldn't ever be considered to be in any blocks
        if (ViewRenderPlan.Companion.getCURRENT() != ViewRenderPlan.Companion.getMAIN()) {
        //#if MC>=11400
        //$$     return Fluids.EMPTY.getDefaultState();
        //$$ }
        //$$ return activeRenderInfo.getFluidState();
        //#else
            return Blocks.AIR.getDefaultState();
        }
        return world.getBlockState(blockPos);
        //#endif
    }
}
