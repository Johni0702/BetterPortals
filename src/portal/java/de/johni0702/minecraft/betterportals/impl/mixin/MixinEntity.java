package de.johni0702.minecraft.betterportals.impl.mixin;

import de.johni0702.minecraft.betterportals.impl.common.PortalManagerImpl;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class MixinEntity {
    //#if MC>=11400
    //$$ // FIXME to reproduce, have the player be pushed to one side by a piston below a lying portal while in the other
    //$$ //  dimension with their head.
    //#else
    @Inject(
            method = "pushOutOfBlocks",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/World;collidesWithAnyBlock(Lnet/minecraft/util/math/AxisAlignedBB;)Z"
            )
    )
    private void beforeCollidesWithAnyBlock(double x, double y, double z, CallbackInfoReturnable<Boolean> ci) {
        PortalManagerImpl.EventHandler.INSTANCE.setCollisionBoxesEntity((Entity) (Object) this);
    }

    @Inject(
            method = "pushOutOfBlocks",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/World;collidesWithAnyBlock(Lnet/minecraft/util/math/AxisAlignedBB;)Z",
                    shift = At.Shift.AFTER
            )
    )
    private void afterCollidesWithAnyBlock(double x, double y, double z, CallbackInfoReturnable<Boolean> ci) {
        PortalManagerImpl.EventHandler.INSTANCE.setCollisionBoxesEntity(null);
    }
    //#endif

    @Inject(method = "isInLava", at = @At("HEAD"), cancellable = true)
    private void isInLava(CallbackInfoReturnable<Boolean> ci) {
        Boolean result = PortalManagerImpl.EventHandler.INSTANCE.isInMaterial((Entity) (Object) this, Material.LAVA);
        if (result != null) {
            ci.setReturnValue(result);
        }
    }
}
