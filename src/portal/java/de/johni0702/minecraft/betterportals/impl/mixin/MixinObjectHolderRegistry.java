//#if MC<11400
package de.johni0702.minecraft.betterportals.impl.mixin;

import de.johni0702.minecraft.betterportals.impl.IObjectHolderRegistry;
import de.johni0702.minecraft.betterportals.impl.client.renderer.PortalRendererHooks;
import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraftforge.registries.ObjectHolderRegistry;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(value = ObjectHolderRegistry.class, remap = false)
public abstract class MixinObjectHolderRegistry implements IObjectHolderRegistry {
    private final List<Function0<Unit>> handlers = new ArrayList<>();

    @Override
    public void addHandler(@NotNull Function0<Unit> handler) {
        handlers.add(handler);
    }

    @Inject(method = "applyObjectHolders", at = @At(value = "FIELD", target = "Lnet/minecraftforge/registries/ObjectHolderRegistry;objectHolders:Ljava/util/List;"))
    private void applyHandlers(CallbackInfo ci) {
        for (Function0<Unit> handler : handlers) {
            handler.invoke();
        }
    }
}
//#endif
