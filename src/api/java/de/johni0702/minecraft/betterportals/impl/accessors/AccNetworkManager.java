package de.johni0702.minecraft.betterportals.impl.accessors;

import io.netty.channel.Channel;
import net.minecraft.network.NetworkManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(NetworkManager.class)
public interface AccNetworkManager {
    @Accessor("channel")
    Channel getNettyChannel();
}
