package de.johni0702.minecraft.view.impl.mixin;

import io.github.opencubicchunks.cubicchunks.core.server.CubeWatcher;
import net.minecraft.entity.player.EntityPlayerMP;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = CubeWatcher.class, remap = false)
public interface AccessorCubeWatcher_CC {
    @Invoker
    void invokeAddPlayer(EntityPlayerMP player);
    @Invoker
    void invokeRemovePlayer(EntityPlayerMP player);
}
