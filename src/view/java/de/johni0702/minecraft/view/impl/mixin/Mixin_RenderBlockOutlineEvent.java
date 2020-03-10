//#if FABRIC>=1
//$$ package de.johni0702.minecraft.view.impl.mixin;
//$$
//$$ import de.johni0702.minecraft.view.impl.client.render.RenderBlockHighlightEvent;
//$$ import net.minecraft.client.render.WorldRenderer;
//$$ import org.spongepowered.asm.mixin.Mixin;
//$$ import org.spongepowered.asm.mixin.injection.At;
//$$ import org.spongepowered.asm.mixin.injection.Inject;
//$$ import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//$$
//$$ @Mixin(WorldRenderer.class)
//$$ public abstract class Mixin_RenderBlockOutlineEvent {
//$$     @Inject(method = "drawHighlightedBlockOutline", at = @At("HEAD"), cancellable = true)
//$$     private void fireRenderBlockOutlineEvent(CallbackInfo ci) {
//$$         RenderBlockHighlightEvent event = new RenderBlockHighlightEvent();
//$$         RenderBlockHighlightEvent.EVENT.invoker().handle(event);
//$$         if (event.isCanceled()) {
//$$             ci.cancel();
//$$         }
//$$     }
//$$ }
//#endif
