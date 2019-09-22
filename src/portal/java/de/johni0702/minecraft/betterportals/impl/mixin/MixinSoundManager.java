package de.johni0702.minecraft.betterportals.impl.mixin;

import de.johni0702.minecraft.betterportals.impl.client.audio.PortalAwareSoundManager;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.ITickableSound;
import net.minecraft.client.audio.SoundManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//#if MC>=11400
//$$ import net.minecraft.client.renderer.ActiveRenderInfo;
//#else
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
//#endif

@Mixin(SoundManager.class)
public abstract class MixinSoundManager {
    //#if MC>=11400
    //$$ @Inject(method = "updateListener", at = @At("HEAD"), remap = false)
    //$$ private void setListener(ActiveRenderInfo listener, CallbackInfo ci) {
    //$$     if (listener.isValid()) {
    //$$         PortalAwareSoundManager.INSTANCE.setListenerPos(listener.getProjectedView());
    //$$     }
    //$$ }
    //#else
    @Inject(method = "setListener(Lnet/minecraft/entity/Entity;F)V", at = @At("HEAD"), remap = false)
    private void setListener(Entity entity, float partialTicks, CallbackInfo ci) {
        if (entity != null) {
            double x = entity.prevPosX + (entity.posX - entity.prevPosX) * partialTicks;
            double y = entity.prevPosY + (entity.posY - entity.prevPosY) * partialTicks + entity.getEyeHeight();
            double z = entity.prevPosZ + (entity.posZ - entity.prevPosZ) * partialTicks;
            PortalAwareSoundManager.INSTANCE.setListenerPos(new Vec3d(x, y, z));
        }
    }
    //#endif

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

    //#if MC>=11400
    //$$ @Redirect(method = "tickNonPaused", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/audio/ITickableSound;getX()F"))
    //#else
    @Redirect(method = "updateAllSounds", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/audio/ITickableSound;getXPosF()F"))
    //#endif
    private float getXPosFConsideringPortals$1(ITickableSound sound) {
        return (float) PortalAwareSoundManager.INSTANCE.getApparentPos(sound).x;
    }

    //#if MC>=11400
    //$$ @Redirect(method = "tickNonPaused", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/audio/ITickableSound;getY()F"))
    //#else
    @Redirect(method = "updateAllSounds", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/audio/ITickableSound;getYPosF()F"))
    //#endif
    private float getYPosFConsideringPortals$1(ITickableSound sound) {
        return (float) PortalAwareSoundManager.INSTANCE.getApparentPos(sound).y;
    }

    //#if MC>=11400
    //$$ @Redirect(method = "tickNonPaused", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/audio/ITickableSound;getZ()F"))
    //#else
    @Redirect(method = "updateAllSounds", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/audio/ITickableSound;getZPosF()F"))
    //#endif
    private float getZPosFConsideringPortals$1(ITickableSound sound) {
        return (float) PortalAwareSoundManager.INSTANCE.getApparentPos(sound).z;
    }
}
