package de.johni0702.minecraft.betterportals.impl.accessors;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.shader.Framebuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Queue;
import java.util.concurrent.FutureTask;

@Mixin(Minecraft.class)
public interface AccMinecraft {
    @Accessor("framebufferMC")
    Framebuffer getFramebuffer();
    @Accessor("framebufferMC")
    void setFramebuffer(Framebuffer value);

    @Accessor
    ItemRenderer getItemRenderer();
    @Accessor
    void setItemRenderer(ItemRenderer value);

    //#if MC<11400
    @Accessor
    Queue<FutureTask<?>> getScheduledTasks();
    @Accessor
    void setScheduledTasks(Queue<FutureTask<?>> value);
    //#endif
}
