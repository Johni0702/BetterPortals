package de.johni0702.minecraft.betterportals.mixin;

import com.mojang.authlib.GameProfile;
import de.johni0702.minecraft.betterportals.common.entity.AbstractPortalEntity;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityPlayerSP.class)
public abstract class MixinEntityPlayerSP extends AbstractClientPlayer {
    public MixinEntityPlayerSP(World worldIn, GameProfile playerProfile) {
        super(worldIn, playerProfile);
    }

    @Inject(
            method = "isOpenBlockSpace",
            at = @At(value = "HEAD"),
            cancellable = true
    )
    private void isOpenBlockSpaceCheckPortals(BlockPos pos, CallbackInfoReturnable<Boolean> ci) {
        ci.setReturnValue(AbstractPortalEntity.EventHandler.INSTANCE.onIsOpenBlockSpace(this, pos));
    }
}
