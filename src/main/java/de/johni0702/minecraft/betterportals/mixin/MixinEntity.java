package de.johni0702.minecraft.betterportals.mixin;

import de.johni0702.minecraft.betterportals.common.entity.AbstractPortalEntity;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class MixinEntity {
    @Inject(
            method = "pushOutOfBlocks",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/World;collidesWithAnyBlock(Lnet/minecraft/util/math/AxisAlignedBB;)Z"
            )
    )
    private void beforeCollidesWithAnyBlock(double x, double y, double z, CallbackInfoReturnable<Boolean> ci) {
        AbstractPortalEntity.EventHandler.INSTANCE.setCollisionBoxesEntity((Entity) (Object) this);
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
        AbstractPortalEntity.EventHandler.INSTANCE.setCollisionBoxesEntity(null);
    }
}
