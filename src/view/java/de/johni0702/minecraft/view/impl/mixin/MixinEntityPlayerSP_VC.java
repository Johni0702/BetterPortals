package de.johni0702.minecraft.view.impl.mixin;

import com.mojang.authlib.GameProfile;
import de.johni0702.minecraft.view.impl.client.ViewEntity;
import de.johni0702.minecraft.view.impl.client.render.ViewCameraEntity;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityPlayerSP.class)
public abstract class MixinEntityPlayerSP_VC extends AbstractClientPlayer {
    public MixinEntityPlayerSP_VC(World worldIn, GameProfile playerProfile) {
        super(worldIn, playerProfile);
    }

    @Dynamic("Override added by Vivecraft to update room origin on position change.")
    @Inject(method = {"setPosition", "func_70107_b"}, at = @At("HEAD"), cancellable = true, remap = false)
    private void suppressRoomMoveForCameraEntity$0(double x, double y, double z, CallbackInfo ci) {
        if ((Object) this instanceof ViewEntity || (Object) this instanceof ViewCameraEntity) {
            ci.cancel();
            super.setPosition(x, y, z);
        }
    }

    @Dynamic("Override added by Vivecraft to update room origin on position change.")
    @Inject(method = {"setPositionAndRotation", "func_70080_a"}, at = @At("HEAD"), cancellable = true, remap = false)
    private void suppressRoomMoveForCameraEntity$1(double x, double y, double z, float yaw, float pitch, CallbackInfo ci) {
        if ((Object) this instanceof ViewEntity || (Object) this instanceof ViewCameraEntity) {
            ci.cancel();
            super.setPositionAndRotation(x, y, z, yaw, pitch);
        }
    }

    @Dynamic("Override added by Vivecraft to update room origin on position change.")
    @Inject(method = {"setLocationAndAngles", "func_70012_b"}, at = @At("HEAD"), cancellable = true, remap = false)
    private void suppressRoomMoveForCameraEntity$2(double x, double y, double z, float yaw, float pitch, CallbackInfo ci) {
        if ((Object) this instanceof ViewEntity || (Object) this instanceof ViewCameraEntity) {
            ci.cancel();
            super.setLocationAndAngles(x, y, z, yaw, pitch);
        }
    }
}
