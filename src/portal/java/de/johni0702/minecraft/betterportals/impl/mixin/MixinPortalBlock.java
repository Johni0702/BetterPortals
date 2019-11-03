// Note: This actually belongs into the `vanilla` source set, it's here just cause Mixin isn't set up over there.
// With forge this isn't necessary because it takes care of replacing Blocks.PORTAL_BLOCK for us.
// Fabric doesn't however and the easiest way to work around this is to at the start of createPortalAt.
//#if FABRIC>=1
//$$ package de.johni0702.minecraft.betterportals.impl.mixin;
//$$
//$$ import net.minecraft.block.Block;
//$$ import net.minecraft.block.PortalBlock;
//$$ import net.minecraft.util.Identifier;
//$$ import net.minecraft.util.math.BlockPos;
//$$ import net.minecraft.util.registry.Registry;
//$$ import net.minecraft.world.IWorld;
//$$ import org.spongepowered.asm.mixin.Mixin;
//$$ import org.spongepowered.asm.mixin.injection.At;
//$$ import org.spongepowered.asm.mixin.injection.Inject;
//$$ import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//$$
//$$ @Mixin(PortalBlock.class)
//$$ public abstract class MixinPortalBlock {
//$$     @Inject(method = "createPortalAt", at = @At("HEAD"), cancellable = true)
//$$     private void createBetterPortalAt(IWorld world, BlockPos pos, CallbackInfoReturnable<Boolean> ci) {
//$$         if (getClass().asSubclass(PortalBlock.class) != PortalBlock.class) {
//$$             return; // still allow other mods to subclass the vanilla portal block
//$$         }
//$$
//$$         Block actualBlock = Registry.BLOCK.get(new Identifier("minecraft", "nether_portal"));
//$$         if (actualBlock == (Object) this) {
//$$             return; // this class is actually registered (did someone replace our replacement?), better not mess with it
//$$         }
//$$
//$$         // Redirect call to correct (probably our) portal block implementation
//$$         ci.cancel();
//$$         ((PortalBlock) actualBlock).createPortalAt(world, pos);
//$$     }
//$$ }
//#endif
