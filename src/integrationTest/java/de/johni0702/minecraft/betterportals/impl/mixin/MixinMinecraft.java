package de.johni0702.minecraft.betterportals.impl.mixin;

import de.johni0702.minecraft.betterportals.impl.IHasMainThread;
import de.johni0702.minecraft.betterportals.impl.MainKt;
import net.minecraft.client.Minecraft;
import net.minecraft.crash.CrashReport;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//#if MC>=11400
//$$ import de.johni0702.minecraft.betterportals.impl.IRender;
//$$ import net.minecraft.client.gui.LoadingGui;
//$$ import org.spongepowered.asm.mixin.Shadow;
//#endif

@SideOnly(Side.CLIENT)
@Mixin(Minecraft.class)
public abstract class MixinMinecraft implements IHasMainThread
    //#if MC>=11400
    //$$ , IRender
    //#endif
{
    //#if MC>=11400
    //$$ @Shadow public LoadingGui loadingGui;
    //$$
    //$$ @Shadow protected abstract void runGameLoop(boolean boolean_1);
    //$$
    //$$ @Shadow private boolean isGamePaused;
    //$$
    //$$ @Override
    //$$ public void invokeRender() {
    //$$     runGameLoop(true);
    //$$ }
    //$$
    //#endif

    @Inject(method = "init", at = @At("HEAD"))
    private void preInitTests(CallbackInfo ci) {
        MainKt.preInitTests((Minecraft) (Object) this);
    }

    @Inject(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/fml/client/FMLClientHandler;finishMinecraftLoading()V", remap = false))
    private void initTests(CallbackInfo ci) {
        MainKt.initTests();
    }

    @Inject(method = "run", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;init()V", shift = At.Shift.AFTER))
    private void runTests(CallbackInfo ci) {
        //#if MC>=11400
        //$$ while (this.loadingGui != null && this.loadingGui.isPauseScreen()) {
        //$$     runGameLoop(true);
        //$$ }
        //#endif

        boolean success = false;
        try {
            if (MainKt.runTests()) {
                success = true;
            }
        } catch (Throwable throwable) {
            CrashReport crashReport = CrashReport.makeCrashReport(throwable, "Running tests");
            System.out.println(crashReport.getCompleteReport());
        }
        exitWithFMLSecurityManagerWorkaround(success);
    }

    /**
     * This needs to be in its own method so we have two stack frames with Minecraft as owning class to work around
     * a Forge "feature" introduced because other mods cannot behave themselves.
     * See FMLSecurityManager
     */
    private void exitWithFMLSecurityManagerWorkaround(boolean success) {
        Runtime.getRuntime().halt(success ? 0 : 1);
    }

    private Thread clientThread;

    /**
     * @reason kotlintest uses a different "main thread" per spec (and test)
     * @author johni0702
     */
    @Overwrite
    //#if FABRIC>=1
    //$$ public Thread getThread() {
    //$$     return clientThread;
    //$$ }
    //#else
    //#if MC>=11400
    //$$ protected Thread getExecutionThread() {
    //$$     return clientThread;
    //$$ }
    //#else
    public boolean isCallingFromMinecraftThread() {
        return Thread.currentThread() == clientThread;
    }
    //#endif
    //#endif

    @Override
    public void setMainThread() {
        clientThread = Thread.currentThread();
    }
}
