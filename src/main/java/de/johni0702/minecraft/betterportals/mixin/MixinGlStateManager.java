package de.johni0702.minecraft.betterportals.mixin;

import de.johni0702.minecraft.betterportals.client.GlStateManagerState;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Fixes the push/popAttrib methods (see MinecraftForge issue #1637).
 * Probably incompatible with mods that hook into state classes (i.e. ReplayMod ODS rendering).
 */
@Mixin(GlStateManager.class)
public abstract class MixinGlStateManager {
    private static final Deque<GlStateManagerState> attribStack = new ArrayDeque<>();

    @Inject(method = "pushAttrib", at = @At("HEAD"), cancellable = true)
    private static void pushAttribOntoStack(CallbackInfo ci) {
        attribStack.push(new GlStateManagerState());
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        ci.cancel();
    }

    @Inject(method = "popAttrib", at = @At("HEAD"))
    private static void popAttribFromStack(CallbackInfo ci) {
        attribStack.pop().restore();
    }
}
