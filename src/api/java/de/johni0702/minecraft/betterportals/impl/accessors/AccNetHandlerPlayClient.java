package de.johni0702.minecraft.betterportals.impl.accessors;

import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.NetworkManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(NetHandlerPlayClient.class)
public interface AccNetHandlerPlayClient {
    @Accessor
    NetworkManager getNetManager();
    @Accessor
    void setNetManager(NetworkManager value);

    @Accessor("clientWorldController")
    WorldClient getWorld();
    @Accessor("clientWorldController")
    void setWorld(WorldClient value);
}
