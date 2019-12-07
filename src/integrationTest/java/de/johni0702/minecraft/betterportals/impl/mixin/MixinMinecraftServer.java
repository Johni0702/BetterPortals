package de.johni0702.minecraft.betterportals.impl.mixin;

import de.johni0702.minecraft.betterportals.impl.IHasMainThread;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.io.IOException;

//#if FABRIC>=1
//#else
//#if MC>=11400
//$$ import net.minecraftforge.fml.server.ServerLifecycleHooks;
//#else
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.StartupQuery;
//#endif
//#endif

@Mixin(MinecraftServer.class)
public abstract class MixinMinecraftServer implements IHasMainThread {
    //#if FABRIC>=1
    //$$ // FIXME
    //#else
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
        this.serverThread = Thread.currentThread();
        //#if MC<11400
        StartupQuery.reset();
        //#endif
        try {
            init();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        //#if MC>=11400
        //$$ ServerLifecycleHooks.handleServerStarted((MinecraftServer) (Object) this);
        //#else
        FMLCommonHandler.instance().handleServerStarted();
        //#endif
        this.serverIsRunning = true;
    }

    @Override
    public void setMainThread() {
        this.serverThread = Thread.currentThread();
    }

    /**
     * @reason tests are run in a single thread to give them full control over tick and network timing
     * @author johni0702
     */
    @Overwrite
    public void initiateShutdown() {
        if (!this.serverRunning) {
            return;
        }
        this.serverRunning = false;
        //#if MC>=11400
        //$$ ServerLifecycleHooks.handleServerStopping((MinecraftServer) (Object) this);
        //$$ ServerLifecycleHooks.expectServerStopped();
        //#else
        FMLCommonHandler.instance().handleServerStopping();
        FMLCommonHandler.instance().expectServerStopped();
        //#endif
        try {
            stopServer();
        } finally {
            //#if MC>=11400
            //$$ ServerLifecycleHooks.handleServerStopped((MinecraftServer) (Object) this);
            //#else
            FMLCommonHandler.instance().handleServerStopped();
            //#endif
            this.serverStopped = true;
        }
    }
    //#endif
}
