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

    // FIXME preprocessor could handle these
    //#if FABRIC>=1
    //$$ private static final String METHOD_PLAY_DELAYED = "play(Lnet/minecraft/client/sound/SoundInstance;I)V";
    //$$ private static final String METHOD_PLAY = "play(Lnet/minecraft/client/sound/SoundInstance;)V";
    //$$ private static final String METHOD_TICK = "tick()V";
    //#else
    private static final String METHOD_PLAY_DELAYED = "playDelayed";
    private static final String METHOD_PLAY = "play";
    private static final String METHOD_TICK = "tickNonPaused";
    //#endif

    @Inject(method = METHOD_PLAY_DELAYED, at = @At("HEAD"), cancellable = true)
    private void recordViewOnPlayDelayedSound(ISound sound, int delay, CallbackInfo ci) {
        if (!PortalAwareSoundManager.INSTANCE.recordView(sound)) {
            ci.cancel();
        }
    }

    @Inject(method = METHOD_PLAY, at = @At("HEAD"), cancellable = true)
    private void recordViewOnPlaySound(ISound sound, CallbackInfo ci) {
        if (!PortalAwareSoundManager.INSTANCE.recordView(sound)) {
            ci.cancel();
        }
    }

    @Inject(method = METHOD_PLAY, at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/audio/ISound;getVolume()F"
    ))
    private void considerPortalsBeforePlay(ISound sound, CallbackInfo ci) {
        PortalAwareSoundManager.INSTANCE.beforePlay(sound);
    }

    @Redirect(method = METHOD_PLAY, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/audio/ISound;getXPosF()F"))
    private float getXPosFConsideringPortals$0(ISound sound) {
        return (float) PortalAwareSoundManager.INSTANCE.getApparentPos(sound).x;
    }

    @Redirect(method = METHOD_PLAY, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/audio/ISound;getYPosF()F"))
    private float getYPosFConsideringPortals$0(ISound sound) {
        return (float) PortalAwareSoundManager.INSTANCE.getApparentPos(sound).y;
    }

    @Redirect(method = METHOD_PLAY, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/audio/ISound;getZPosF()F"))
    private float getZPosFConsideringPortals$0(ISound sound) {
        return (float) PortalAwareSoundManager.INSTANCE.getApparentPos(sound).z;
    }

    //#if MC>=11400
    //$$ @Redirect(method = METHOD_TICK, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/audio/ITickableSound;getX()F"))
    //#else
    @Redirect(method = "updateAllSounds", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/audio/ITickableSound;getXPosF()F"))
    //#endif
    private float getXPosFConsideringPortals$1(ITickableSound sound) {
        return (float) PortalAwareSoundManager.INSTANCE.getApparentPos(sound).x;
    }

    //#if MC>=11400
    //$$ @Redirect(method = METHOD_TICK, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/audio/ITickableSound;getY()F"))
    //#else
    @Redirect(method = "updateAllSounds", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/audio/ITickableSound;getYPosF()F"))
    //#endif
    private float getYPosFConsideringPortals$1(ITickableSound sound) {
        return (float) PortalAwareSoundManager.INSTANCE.getApparentPos(sound).y;
    }

    //#if MC>=11400
    //$$ @Redirect(method = METHOD_TICK, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/audio/ITickableSound;getZ()F"))
    //#else
    @Redirect(method = "updateAllSounds", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/audio/ITickableSound;getZPosF()F"))
    //#endif
    private float getZPosFConsideringPortals$1(ITickableSound sound) {
        return (float) PortalAwareSoundManager.INSTANCE.getApparentPos(sound).z;
    }
}
