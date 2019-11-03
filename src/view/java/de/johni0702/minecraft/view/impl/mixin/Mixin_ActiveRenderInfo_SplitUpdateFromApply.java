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
//#if FABRIC>=1
//$$ import de.johni0702.minecraft.view.impl.client.render.CameraSetupEvent;
//#else
//$$ import net.minecraft.client.Minecraft;
//$$ import net.minecraftforge.client.event.EntityViewRenderEvent;
//$$ import net.minecraftforge.common.MinecraftForge;
//#endif
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
//$$         // TODO this breaks other mods which manipulate the camera, we should instead only void calls which have the
//$$         //      same camera entity in the same position. will wait for such a mod before doing that though
//$$         if (ViewRenderManager.Companion.getINSTANCE().getCurrent() != null) {
//$$             ci.cancel();
//$$
//$$             apply(partialTicks);
//$$         }
//$$     }
//$$
//$$     private void apply(float partialTicks) {
//$$         // Forge doesn't yet fire this but we need it, so guess we'll be adding it ourselves until Forge decides to do
//$$         // so. See https://github.com/MinecraftForge/MinecraftForge/issues/5911
        //#if FABRIC>=1
        //$$ CameraSetupEvent event = new CameraSetupEvent(this.yaw, this.pitch, 0);
        //$$ CameraSetupEvent.EVENT.invoker().handle(event);
        //#else
        //$$ EntityViewRenderEvent.CameraSetup event = new EntityViewRenderEvent.CameraSetup(
        //$$         Minecraft.getInstance().gameRenderer,
        //$$         (ActiveRenderInfo) (Object) this,
        //$$         partialTicks,
        //$$         this.yaw,
        //$$         this.pitch,
        //$$         0
        //$$ );
        //$$ MinecraftForge.EVENT_BUS.post(event);
        //#endif
//$$
//$$         GlStateManager.rotatef(event.getRoll(), 0.0F, 0.0F, 1.0F);
//$$         GlStateManager.rotatef(event.getPitch(), 1.0F, 0.0F, 0.0F);
//$$         GlStateManager.rotatef(event.getYaw() + 180.0F, 0.0F, 1.0F, 0.0F);
//$$     }
//$$
//$$     @Inject(method = "update", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/GlStateManager;rotatef(FFFF)V"), cancellable = true)
//$$     private void applyWithEvent(IBlockReader worldIn, Entity renderViewEntity, boolean thirdPersonIn, boolean thirdPersonReverseIn, float partialTicks, CallbackInfo ci) {
//$$         ci.cancel();
//$$
//$$         apply(partialTicks);
//$$     }
//$$ }
//#endif
