//#if FABRIC<=0
package de.johni0702.minecraft.view.impl.mixin;

import kotlin.Triple;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.client.ForgeHooksClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.WeakHashMap;

@Mixin(value = ForgeHooksClient.class, remap = false)
public abstract class MixinForgeHooksClient {

    @Shadow private static boolean skyInit;
    @Shadow private static int skyRGBMultiplier;
    @Shadow private static int skyX;
    @Shadow private static int skyZ;

    private static WeakHashMap<World, Triple<Integer, Integer, Integer>> skyColorCache = new WeakHashMap<>();

    @Inject(method = "getSkyBlendColour", at = @At("HEAD"))
    private static void loadSkyColorCache(World world, BlockPos center, CallbackInfoReturnable<Integer> ci) {
        Triple<Integer, Integer, Integer> cache = skyColorCache.get(world);
        if (cache != null) {
            skyInit = true;
            skyX = cache.component1();
            skyZ = cache.component2();
            skyRGBMultiplier = cache.component3();
        } else {
            skyInit = false;
        }
    }

    @Inject(method = "getSkyBlendColour", at = @At(value = "RETURN", ordinal = 1))
    private static void storeSkyColorCache(World world, BlockPos center, CallbackInfoReturnable<Integer> ci) {
        skyColorCache.put(world, new Triple<>(skyX, skyZ, skyRGBMultiplier));
    }
}
//#endif
