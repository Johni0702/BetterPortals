package de.johni0702.minecraft.betterportals.impl.mixin;

import de.johni0702.minecraft.betterportals.common.Mat4d;
import kotlin.Pair;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import javax.vecmath.Matrix4d;

import static de.johni0702.minecraft.betterportals.common.ExtensionsKt.getSyncPos;
import static de.johni0702.minecraft.betterportals.common.ExtensionsKt.inverse;
import static de.johni0702.minecraft.betterportals.common.ExtensionsKt.rayTraceBlocksWithPortals;
import static de.johni0702.minecraft.betterportals.common.ExtensionsKt.rayTracePortals;
import static de.johni0702.minecraft.betterportals.common.ExtensionsKt.times;
import static de.johni0702.minecraft.betterportals.common.ExtensionsKt.toMC;
import static de.johni0702.minecraft.betterportals.common.ExtensionsKt.toPoint;

//#if MC>=11400
//$$ import net.minecraft.client.renderer.ActiveRenderInfo;
//$$ import net.minecraft.util.Direction;
//$$ import net.minecraft.util.math.BlockPos;
//$$ import net.minecraft.util.math.BlockRayTraceResult;
//$$ import net.minecraft.util.math.RayTraceContext;
//$$ import net.minecraft.world.IBlockReader;
//#else
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.multiplayer.WorldClient;
//#endif

//#if MC>=11400
//$$ @Mixin(ActiveRenderInfo.class)
//#else
@Mixin(EntityRenderer.class)
//#endif
@SideOnly(Side.CLIENT)
public abstract class MixinEntityRenderer {
    //#if MC>=11400
    //$$ @Redirect(
    //$$         method = "calcCameraDistance",
    //$$         at = @At(
    //$$                 value = "INVOKE",
    //$$                 target = "Lnet/minecraft/world/IBlockReader;rayTraceBlocks(Lnet/minecraft/util/math/RayTraceContext;)Lnet/minecraft/util/math/BlockRayTraceResult;"
    //$$         )
    //$$ )
    //$$ private BlockRayTraceResult doRayTraceBlocksWithPortals(IBlockReader iBlockReader, RayTraceContext context) {
    //$$     if (!(iBlockReader instanceof World)) {
    //$$         return iBlockReader.rayTraceBlocks(context);
    //$$     }
    //$$     World world = (World) iBlockReader;
    //$$     Vec3d start = context.func_222253_b();
    //$$     Vec3d end = context.func_222250_a();
    //#else
    @Redirect(
            method = "orientCamera",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/multiplayer/WorldClient;rayTraceBlocks(Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/util/math/Vec3d;)Lnet/minecraft/util/math/RayTraceResult;"
            )
    )
    private RayTraceResult doRayTraceBlocksWithPortals(WorldClient clientWorld, Vec3d start, Vec3d end) {
        World world = clientWorld;
    //#endif
        Pair<World, Matrix4d> result;
        Entity viewEntity = Minecraft.getMinecraft().getRenderViewEntity();
        Entity vehicle = viewEntity.getLowestRidingEntity();
        Vec3d vehiclePos = getSyncPos(vehicle).addVector(0, vehicle.getEyeHeight(), 0);
        Vec3d eyePos = viewEntity.getPositionVector().addVector(0, viewEntity.getEyeHeight(), 0);
        Matrix4d matrix = Mat4d.id();

        if (!vehiclePos.equals(eyePos)) {
            result = rayTracePortals(world, vehiclePos, eyePos);
            world = result.getFirst();
            matrix = times(matrix, inverse(result.getSecond()));
            eyePos = toMC(times(result.getSecond(), toPoint(eyePos)));
            start = toMC(times(result.getSecond(), toPoint(start)));
            end = toMC(times(result.getSecond(), toPoint(end)));
        }

        if (!eyePos.equals(start)) {
            result = rayTracePortals(world, eyePos, start);
            world = result.getFirst();
            matrix = times(matrix, inverse(result.getSecond()));
            start = toMC(times(result.getSecond(), toPoint(start)));
            end = toMC(times(result.getSecond(), toPoint(end)));
        }

        // Calling code only uses hitVec, so we need to transform only it
        //#if MC>=11400
        //$$ RayTraceResult rayResult = rayTraceBlocksWithPortals(world, new RayTraceContext(start, end, RayTraceContext.BlockMode.COLLIDER, RayTraceContext.FluidMode.NONE, viewEntity));
        //$$ if (rayResult.getType() != RayTraceResult.Type.MISS) {
        //$$     return new BlockRayTraceResult(toMC(times(matrix, toPoint(rayResult.getHitVec()))), Direction.DOWN, BlockPos.ZERO, false);
        //$$ } else {
        //$$     return BlockRayTraceResult.createMiss(Vec3d.ZERO, Direction.DOWN, BlockPos.ZERO);
        //$$ }
        //#else
        RayTraceResult rayResult = rayTraceBlocksWithPortals(world, start, end, false, false, false);
        if (rayResult != null) {
            rayResult.hitVec = toMC(times(matrix, toPoint(rayResult.hitVec)));
        }
        return rayResult;
        //#endif
    }
}
