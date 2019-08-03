package de.johni0702.minecraft.betterportals.impl.mixin;

import de.johni0702.minecraft.betterportals.impl.client.audio.PortalAwareSoundManager;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.ITickableSound;
import net.minecraft.client.audio.SoundManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SoundManager.class)
public abstract class MixinSoundManager {
    @Inject(method = "setListener(Lnet/minecraft/entity/Entity;F)V", at = @At("HEAD"), remap = false)
    private void setListener(Entity entity, float partialTicks, CallbackInfo ci) {
        if (entity != null) {
            double x = entity.prevPosX + (entity.posX - entity.prevPosX) * partialTicks;
            double y = entity.prevPosY + (entity.posY - entity.prevPosY) * partialTicks + entity.getEyeHeight();
            double z = entity.prevPosZ + (entity.posZ - entity.prevPosZ) * partialTicks;
            PortalAwareSoundManager.INSTANCE.setListenerPos(new Vec3d(x, y, z));
        }
    }

    @Inject(method = "playDelayedSound", at = @At("HEAD"), cancellable = true)
    private void recordViewOnPlayDelayedSound(ISound sound, int delay, CallbackInfo ci) {
        if (!PortalAwareSoundManager.INSTANCE.recordView(sound)) {
            ci.cancel();
        }
    }

    @Inject(method = "playSound", at = @At("HEAD"), cancellable = true)
    private void recordViewOnPlaySound(ISound sound, CallbackInfo ci) {
        if (!PortalAwareSoundManager.INSTANCE.recordView(sound)) {
            ci.cancel();
        }
    }

    @Inject(method = "playSound", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/audio/ISound;getVolume()F"
    ))
    private void considerPortalsBeforePlay(ISound sound, CallbackInfo ci) {
        PortalAwareSoundManager.INSTANCE.beforePlay(sound);
    }

    @Redirect(method = "playSound", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/audio/ISound;getXPosF()F"))
    private float getXPosFConsideringPortals$0(ISound sound) {
        return (float) PortalAwareSoundManager.INSTANCE.getApparentPos(sound).x;
    }

    @Redirect(method = "playSound", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/audio/ISound;getYPosF()F"))
    private float getYPosFConsideringPortals$0(ISound sound) {
        return (float) PortalAwareSoundManager.INSTANCE.getApparentPos(sound).y;
    }

    @Redirect(method = "playSound", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/audio/ISound;getZPosF()F"))
    private float getZPosFConsideringPortals$0(ISound sound) {
        return (float) PortalAwareSoundManager.INSTANCE.getApparentPos(sound).z;
    }

    @Redirect(method = "updateAllSounds", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/audio/ITickableSound;getXPosF()F"))
    private float getXPosFConsideringPortals$1(ITickableSound sound) {
        return (float) PortalAwareSoundManager.INSTANCE.getApparentPos(sound).x;
    }

    @Redirect(method = "updateAllSounds", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/audio/ITickableSound;getYPosF()F"))
    private float getYPosFConsideringPortals$1(ITickableSound sound) {
        return (float) PortalAwareSoundManager.INSTANCE.getApparentPos(sound).y;
    }

    @Redirect(method = "updateAllSounds", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/audio/ITickableSound;getZPosF()F"))
    private float getZPosFConsideringPortals$1(ITickableSound sound) {
        return (float) PortalAwareSoundManager.INSTANCE.getApparentPos(sound).z;
    }
}
