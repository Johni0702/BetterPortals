// Used to be for fixing a vanilla bug (see usages of interface) but has been fixed in 1.13/14 \o/
//#if MC<11400
package de.johni0702.minecraft.view.impl.mixin;

import de.johni0702.minecraft.view.impl.IChunkCompileTaskGenerator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.chunk.ChunkCompileTaskGenerator;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SideOnly(Side.CLIENT)
@Mixin(ChunkCompileTaskGenerator.class)
public abstract class MixinChunkCompileTaskGenerator implements IChunkCompileTaskGenerator {
    private Vec3d viewerEyePos;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void captureViewerPos(RenderChunk renderChunkIn, ChunkCompileTaskGenerator.Type typeIn, double distanceSqIn, CallbackInfo ci) {
        // Capture viewer eye position now, so we can later access it from the worker thread
        viewerEyePos = Minecraft.getMinecraft().getRenderViewEntity().getPositionEyes(0f);
    }

    @NotNull
    @Override
    public Vec3d getViewerEyePos() {
        return viewerEyePos;
    }
}
//#endif
