package de.johni0702.minecraft.betterportals.impl.mixin;

import de.johni0702.minecraft.betterportals.impl.IHasMainThread;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.StartupQuery;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.io.IOException;

@Mixin(MinecraftServer.class)
public abstract class MixinMinecraftServer implements IHasMainThread {
    @Shadow public abstract boolean init() throws IOException;

    @Shadow private boolean serverIsRunning;

    @Shadow private Thread serverThread;

    @Shadow private boolean serverRunning;

    @Shadow public abstract void stopServer();

    @Shadow private boolean serverStopped;

    /**
     * @reason tests are run in a single thread to give them full control over tick and network timing
     * @author johni0702
     */
    @Overwrite
    public void startServerThread() {
        serverThread = Thread.currentThread();
        StartupQuery.reset();
        try {
            init();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        FMLCommonHandler.instance().handleServerStarted();
        serverIsRunning = true;
    }

    @Override
    public void setMainThread() {
        serverThread = Thread.currentThread();
    }

    /**
     * @reason tests are run in a single thread to give them full control over tick and network timing
     * @author johni0702
     */
    @Overwrite
    public void initiateShutdown() {
        if (!serverRunning) {
            return;
        }
        serverRunning = false;
        FMLCommonHandler.instance().handleServerStopping();
        FMLCommonHandler.instance().expectServerStopped();
        try {
            stopServer();
        } finally {
            FMLCommonHandler.instance().handleServerStopped();
            serverStopped = true;
        }
    }
}
