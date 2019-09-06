package de.johni0702.minecraft.view.impl.mixin;

import de.johni0702.minecraft.view.impl.IWorldsManagerHolder;
import de.johni0702.minecraft.view.impl.server.ServerWorldsManagerImpl;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(NetHandlerPlayServer.class)
public class MixinNetHandlerPlayerServer implements IWorldsManagerHolder {
    @Shadow @Final private MinecraftServer serverController;

    @Nullable // lazily initialized in case getWorldsManager is overridden (like for view entities)
    private ServerWorldsManagerImpl viewManager;

    @NotNull
    @Override
    public ServerWorldsManagerImpl getWorldsManager() {
        if (viewManager == null) {
            viewManager = new ServerWorldsManagerImpl(this.serverController, (NetHandlerPlayServer)(Object) this);
        }
        return viewManager;
    }
}
