package de.johni0702.minecraft.betterportals.impl.mixin;

import de.johni0702.minecraft.betterportals.impl.client.PostSetupFogEvent;
import kotlin.Pair;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.vecmath.Matrix4d;

import static de.johni0702.minecraft.betterportals.common.ExtensionsKt.rayTraceBlocksWithPortals;
import static de.johni0702.minecraft.betterportals.common.ExtensionsKt.rayTracePortals;
import static de.johni0702.minecraft.betterportals.common.ExtensionsKt.times;
import static de.johni0702.minecraft.betterportals.common.ExtensionsKt.toMC;
import static de.johni0702.minecraft.betterportals.common.ExtensionsKt.toPoint;

@Mixin(EntityRenderer.class)
public abstract class MixinEntityRenderer {
    @Shadow @Final private Minecraft mc;

    @Inject(method = "setupFog", at = @At("RETURN"))
    private void postSetupFogInView(int start, float partialTicks, CallbackInfo ci) {
        MinecraftForge.EVENT_BUS.post(new PostSetupFogEvent());
    }

    @Redirect(
            method = "orientCamera",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/multiplayer/WorldClient;rayTraceBlocks(Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/math/Vec3d;)Lnet/minecraft/util/math/RayTraceResult;"
            )
    )
    private RayTraceResult doRayTraceBlocksWithPortals(WorldClient world, Vec3d start, Vec3d end) {
        Entity viewEntity = mc.getRenderViewEntity();
        Vec3d eyePos = viewEntity.getPositionVector().addVector(0, viewEntity.getEyeHeight(), 0);
        Pair<World, Matrix4d> result = rayTracePortals(world, eyePos, start);
        World localWorld = result.getFirst();
        start = toMC(times(result.getSecond(), toPoint(start)));
        end = toMC(times(result.getSecond(), toPoint(end)));
        return rayTraceBlocksWithPortals(localWorld, start, end, false, false, false);
    }
}
