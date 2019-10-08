package de.johni0702.minecraft.view.impl.mixin;

import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Invoker;

@Pseudo
@Mixin(targets = "net.minecraft.client.renderer.EntityRenderer")
public interface AccessorEntityRenderer_VC {
    @Dynamic("Added by Vivecraft")
    @Invoker(remap = false)
    void invokeApplyCameraDepth(boolean reverse);
}
