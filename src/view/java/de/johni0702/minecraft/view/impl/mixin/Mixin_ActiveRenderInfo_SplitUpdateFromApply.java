//#if MC>=11400
//$$ package de.johni0702.minecraft.view.impl.mixin;
//$$
//$$ import com.mojang.blaze3d.platform.GlStateManager;
//$$ import de.johni0702.minecraft.view.impl.client.render.ViewRenderManager;
//$$ import net.minecraft.client.renderer.ActiveRenderInfo;
//$$ import net.minecraft.entity.Entity;
//$$ import net.minecraft.world.IBlockReader;
//$$ import org.spongepowered.asm.mixin.Mixin;
//$$ import org.spongepowered.asm.mixin.Shadow;
//$$ import org.spongepowered.asm.mixin.injection.At;
//$$ import org.spongepowered.asm.mixin.injection.Inject;
//$$ import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//$$
//$$ @Mixin(ActiveRenderInfo.class)
//$$ public abstract class Mixin_ActiveRenderInfo_SplitUpdateFromApply {
//$$     @Shadow public float yaw;
//$$     @Shadow public float pitch;
//$$
//$$     @Inject(method = "update", at = @At("HEAD"), cancellable = true)
//$$     private void skipUpdateDuringRendering(IBlockReader worldIn, Entity renderViewEntity, boolean thirdPersonIn, boolean thirdPersonReverseIn, float partialTicks, CallbackInfo ci) {
//$$         // We only want to update (i.e. determine third-person pos) once, at the very start of the rendering process,
//$$         // not on each render pass.
//$$         // Updating the pos/rot for each render pass is done manually in Mixin_ActiveRenderInfo_From_Camera.
//$$         // TODO this breaks other mods which manipulate the camera, we should instead only void the first call
//$$         //      in each pass. will wait for such a mod before doing that though
//$$         if (ViewRenderManager.Companion.getINSTANCE().getCurrent() != null) {
//$$             ci.cancel();
//$$
//$$             // TODO roll, anyone?
//$$             GlStateManager.rotatef(this.pitch, 1.0F, 0.0F, 0.0F);
//$$             GlStateManager.rotatef(this.yaw + 180.0F, 0.0F, 1.0F, 0.0F);
//$$         }
//$$     }
//$$ }
//#endif
