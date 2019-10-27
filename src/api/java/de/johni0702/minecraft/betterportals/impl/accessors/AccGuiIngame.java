package de.johni0702.minecraft.betterportals.impl.accessors;

import net.minecraft.client.gui.GuiBossOverlay;
import net.minecraft.client.gui.GuiIngame;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GuiIngame.class)
public interface AccGuiIngame {
    @Accessor
    GuiBossOverlay getOverlayBoss();
    @Accessor
    void setOverlayBoss(GuiBossOverlay value);
}
