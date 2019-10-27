package de.johni0702.minecraft.betterportals.impl.accessors;

import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(NetHandlerPlayServer.class)
public interface AccNetHandlerPlayServer {
    @Invoker
    void invokeCaptureCurrentPosition();

    @Accessor
    Vec3d getTargetPos();
}
