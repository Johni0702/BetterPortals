package de.johni0702.minecraft.betterportals.impl.mixin;

import de.johni0702.minecraft.betterportals.impl.common.PortalManagerImpl;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

//#if MC>=11400
//$$ import net.minecraft.entity.EntityType;
//$$ import net.minecraft.entity.LivingEntity;
//$$ import net.minecraft.entity.player.PlayerEntity;
//$$ import net.minecraft.world.World;
//#else
import com.mojang.authlib.GameProfile;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.WorldClient;
//#endif

//#if MC>=11400
//$$ @Mixin(PlayerEntity.class)
//$$ public abstract class MixinEntityPlayerSP extends LivingEntity {
//$$     protected MixinEntityPlayerSP(EntityType<? extends LivingEntity> type, World worldIn) {
//$$         super(type, worldIn);
//$$     }
//#else
@Mixin(EntityPlayerSP.class)
public abstract class MixinEntityPlayerSP extends AbstractClientPlayer {
    public MixinEntityPlayerSP(WorldClient worldIn, GameProfile playerProfile) {
        super(worldIn, playerProfile);
    }
//#endif

    @Inject(
            //#if MC>=11400
            //$$ method = "isNormalCube",
            //#else
            method = "isOpenBlockSpace",
            //#endif
            at = @At(value = "HEAD"),
            cancellable = true
    )
    private void isOpenBlockSpaceCheckPortals(BlockPos pos, CallbackInfoReturnable<Boolean> ci) {
        ci.setReturnValue(PortalManagerImpl.EventHandler.INSTANCE.onIsOpenBlockSpace(this, pos));
    }
}
