package de.johni0702.minecraft.betterportals.impl.accessors;

import net.minecraft.client.shader.ShaderManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ShaderManager.class)
public interface AccShaderManager {
    @Accessor
    static ShaderManager getStaticShaderManager() { throw new Error("AccShaderManager did not apply"); }
    @Accessor
    String getProgramFilename();
}
