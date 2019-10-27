// Has been fixed in Vanilla 1.13/14 \o/
//#if MC<11400
package de.johni0702.minecraft.view.impl.mixin;

import de.johni0702.minecraft.view.impl.DummyEntity;
import de.johni0702.minecraft.view.impl.IChunkCompileTaskGenerator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.chunk.ChunkCompileTaskGenerator;
import net.minecraft.client.renderer.chunk.ChunkRenderWorker;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChunkRenderWorker.class)
public abstract class MixinChunkRenderWorker {
    private ChunkCompileTaskGenerator task;

    @Inject(method = "processTask", at = @At("HEAD"))
    private void captureTask(ChunkCompileTaskGenerator generator, CallbackInfo ci) {
        this.task = generator;
    }

    // For thread safety
    @Redirect(
            method = "processTask",
            at = @At(value = "NEW", target = "net/minecraft/util/math/BlockPos")
    )
    private BlockPos viewerBlockPos(Entity entity) {
        return new BlockPos(((IChunkCompileTaskGenerator) this.task).getViewerEyePos());
    }

    // See the transparency patches in MixinRenderGlobal
    @Redirect(
            method = "processTask",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;getRenderViewEntity()Lnet/minecraft/entity/Entity;")
    )
    private Entity getPlayerZForTransparencySort(Minecraft mc) {
        Entity entity = new DummyEntity();
        Vec3d pos = ((IChunkCompileTaskGenerator) task).getViewerEyePos();
        entity.posX = pos.x;
        entity.posY = pos.y - entity.getEyeHeight();
        entity.posZ = pos.z;
        return entity;
    }
}
//#endif
