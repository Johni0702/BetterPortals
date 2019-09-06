package de.johni0702.minecraft.view.impl.mixin;

import de.johni0702.minecraft.view.impl.server.ViewEntity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.SPacketBlockBreakAnim;
import net.minecraft.server.management.PlayerList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

//#if MC>=11400
//$$ import net.minecraft.entity.player.ServerPlayerEntity;
//#else
import net.minecraft.world.ServerWorldEventHandler;
//#endif

//#if MC>=11400
//$$ @Mixin(ServerWorld.class)
//#else
@Mixin(ServerWorldEventHandler.class)
//#endif
public class MixinServerWorldEventHandler {
    //#if MC>=11400
    //$$ @Shadow @Final private List<ServerPlayerEntity> players;
    //#else
    @Shadow @Final private WorldServer world;
    //#endif

    /**
     * For some unknown reason (probably because it only gets the entity id, for some reason) this method does not call
     * {@link PlayerList#sendToAllNearExcept(EntityPlayer, double, double, double, double, int, Packet)}
     * but instead basically copies that code.
     * We need to inject into this for the same reason as explained in {@link MixinPlayerList}.
     */
    @Inject(method = "sendBlockBreakProgress", at = @At("HEAD"))
    private void sendBlockBreakProgress(int breakerId, BlockPos pos, int progress, CallbackInfo ci) {
        //#if MC<11400
        List<EntityPlayer> players = world.playerEntities;
        //#endif
        for (EntityPlayer entity : players) {
            if (entity instanceof ViewEntity) { // Note: check was omitted because view entity does never break stuff
                double dx = pos.getX() - entity.posX;
                double dy = pos.getY() - entity.posY;
                double dz = pos.getZ() - entity.posZ;
                if (dx * dx + dy * dy + dz * dz < 1024) {
                    ((ViewEntity) entity).connection.sendPacket(new SPacketBlockBreakAnim(breakerId, pos, progress));
                }
            }
        }
    }
}
