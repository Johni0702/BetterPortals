package de.johni0702.minecraft.betterportals.impl.mixin;

import com.mojang.authlib.GameProfile;
import de.johni0702.minecraft.betterportals.impl.common.PortalManagerImpl;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityPlayerSP.class)
public abstract class MixinEntityPlayerSP extends AbstractClientPlayer {
    public MixinEntityPlayerSP(WorldClient worldIn, GameProfile playerProfile) {
        super(worldIn, playerProfile);
    }

    @Inject(
            method = "isOpenBlockSpace",
            at = @At(value = "HEAD"),
            cancellable = true
    )
    private void isOpenBlockSpaceCheckPortals(BlockPos pos, CallbackInfoReturnable<Boolean> ci) {
        ci.setReturnValue(PortalManagerImpl.EventHandler.INSTANCE.onIsOpenBlockSpace(this, pos));
    }
}
