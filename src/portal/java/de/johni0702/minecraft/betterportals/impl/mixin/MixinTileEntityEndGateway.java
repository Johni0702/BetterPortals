package de.johni0702.minecraft.betterportals.impl.mixin;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityEndGateway;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

// Note: This actually belongs to the vanilla module but it doesn't have any other mixins, so putting it here is easier.
@Mixin(TileEntityEndGateway.class)
public abstract class MixinTileEntityEndGateway extends TileEntity {
    @Shadow private BlockPos exitPortal;

    @Redirect(method = "teleportEntity",
            at = @At(value = "FIELD", target = "Lnet/minecraft/tileentity/TileEntityEndGateway;exitPortal:Lnet/minecraft/util/math/BlockPos;", ordinal = 2))
    private BlockPos getBetterPortalsEndSpawn(TileEntityEndGateway self) {
        for (int i = 0; i < 10; i++) {
            if (this.world.getBlockState(this.exitPortal.down(i)).isFullBlock()) {
                // Solid block less than 10 below the destination? Good enough, let's use it.
                return this.exitPortal;
            }
        }
        // Using the real exit would probably kill or harm the player, let's instead put them on the island.
        return this.world.getTopSolidOrLiquidBlock(new BlockPos(10, 0, 0));
    }
}
