package de.johni0702.minecraft.view.impl.mixin;

import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import de.johni0702.minecraft.view.impl.client.ClientWorldsManagerImpl;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.server.management.PlayerProfileCache;
import net.minecraft.util.datafix.DataFixer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.lib.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;
import java.net.Proxy;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

//#if MC>=11400
//$$ import net.minecraft.command.Commands;
//$$ import net.minecraft.world.chunk.listener.IChunkStatusListenerFactory;
//#else
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.world.storage.WorldInfo;
//#endif

@SideOnly(Side.CLIENT)
@Mixin(IntegratedServer.class)
public abstract class MixinIntegratedServer extends MinecraftServer implements ClientWorldsManagerImpl.IIntegratedServer {
    //#if MC>=11400
    //$$ public MixinIntegratedServer(File p_i50590_1_, Proxy p_i50590_2_, DataFixer p_i50590_3_, Commands p_i50590_4_, YggdrasilAuthenticationService p_i50590_5_, MinecraftSessionService p_i50590_6_, GameProfileRepository p_i50590_7_, PlayerProfileCache p_i50590_8_, IChunkStatusListenerFactory p_i50590_9_, String p_i50590_10_) {
    //$$     super(p_i50590_1_, p_i50590_2_, p_i50590_3_, p_i50590_4_, p_i50590_5_, p_i50590_6_, p_i50590_7_, p_i50590_8_, p_i50590_9_, p_i50590_10_);
    //$$ }
    //#else
    public MixinIntegratedServer(File anvilFileIn, Proxy proxyIn, DataFixer dataFixerIn, YggdrasilAuthenticationService authServiceIn, MinecraftSessionService sessionServiceIn, GameProfileRepository profileRepoIn, PlayerProfileCache profileCacheIn) {
        super(anvilFileIn, proxyIn, dataFixerIn, authServiceIn, sessionServiceIn, profileRepoIn, profileCacheIn);
    }
    //#endif

    //
    // Fixing thread-safety issues
    //

    // Note: Cannot use addScheduledTask as that could block the client thread for an extended amount of time
    private final Queue<Runnable> clientStateUpdates = new ConcurrentLinkedQueue<>();
    private int clientRenderDistanceChunks = 10;
    //#if MC<11400
    private WorldClient clientWorld; // for null-checking only
    private WorldInfo clientWorldInfo;
    //#endif

    @Override
    public void updateClientState(@NotNull Minecraft mc) {
        int clientRenderDistanceChunks = mc.gameSettings.renderDistanceChunks;
        //#if MC<11400
        WorldClient clientWorld = mc.world;
        WorldInfo clientWorldInfo = clientWorld == null ? null : new WorldInfo(clientWorld.getWorldInfo());
        //#endif
        clientStateUpdates.offer(() -> {
            this.clientRenderDistanceChunks = clientRenderDistanceChunks;
            //#if MC<11400
            this.clientWorld = clientWorld;
            this.clientWorldInfo = clientWorldInfo;
            //#endif
        });
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void pollClientState(CallbackInfo ci) {
        Runnable update;
        while ((update = clientStateUpdates.poll()) != null) {
            update.run();
        }
    }

    @Redirect(method = "tick", at = @At(value = "FIELD", opcode = Opcodes.GETFIELD, target = "Lnet/minecraft/client/settings/GameSettings;renderDistanceChunks:I"))
    private int getClientRenderDistanceChunks(GameSettings gameSettings) {
        return clientRenderDistanceChunks;
    }

    //#if MC>=11400
    //$$ // This is done via a packet now, as it should be. Render distance still isn't..
    //#else
    @Redirect(method = "tick", at = @At(value = "FIELD", opcode = Opcodes.GETFIELD, target = "Lnet/minecraft/client/Minecraft;world:Lnet/minecraft/client/multiplayer/WorldClient;"))
    private WorldClient getClientWorld(Minecraft minecraft) {
        return clientWorld;
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/WorldClient;getWorldInfo()Lnet/minecraft/world/storage/WorldInfo;"))
    private WorldInfo getClientWorldInfo(WorldClient client) {
        return clientWorldInfo;
    }
    //#endif
}
