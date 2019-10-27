package de.johni0702.minecraft.view.impl.mixin;

import de.johni0702.minecraft.view.impl.client.render.ViewCameraEntity;
import net.minecraft.client.entity.AbstractClientPlayer;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(AbstractClientPlayer.class)
public abstract class MixinAbstractClientPlayer_VC {
    // Workaround for https://github.com/jrbudda/Vivecraft_112/issues/129
    // Once that issue is fixed, this is no longer strictly needed (though it wouldn't hurt either).
    @Dynamic("Patched in by Vivecraft")
    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/optifine/http/FileDownloadThread;start()V", remap = false), require = 0)
    private void downloadPatreonFileExceptForCameraEntity(@Coerce Thread thread) {
        if ((Object) this instanceof ViewCameraEntity) {
            return;
        }
        thread.start();
    }
}
