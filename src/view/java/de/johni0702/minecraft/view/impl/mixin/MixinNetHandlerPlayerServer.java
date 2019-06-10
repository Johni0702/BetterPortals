package de.johni0702.minecraft.view.impl.mixin;

import de.johni0702.minecraft.view.impl.IViewManagerHolder;
import de.johni0702.minecraft.view.impl.server.ServerViewManagerImpl;
import de.johni0702.minecraft.view.server.ServerViewManager;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(NetHandlerPlayServer.class)
public class MixinNetHandlerPlayerServer implements IViewManagerHolder {
    @Shadow @Final private MinecraftServer serverController;

    @Nullable // lazily initialized in case getViewManager is overridden (like for view entities)
    private ServerViewManager viewManager;

    @NotNull
    @Override
    public ServerViewManager getViewManager() {
        if (viewManager == null) {
            viewManager = new ServerViewManagerImpl(serverController, (NetHandlerPlayServer)(Object) this);
        }
        return viewManager;
    }
}
