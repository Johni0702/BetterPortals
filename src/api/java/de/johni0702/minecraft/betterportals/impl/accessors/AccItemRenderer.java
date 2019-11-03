package de.johni0702.minecraft.betterportals.impl.accessors;

import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ItemRenderer.class)
public interface AccItemRenderer {
    @Accessor
    ItemStack getItemStackMainHand();
    @Accessor
    void setItemStackMainHand(ItemStack value);
    @Accessor
    ItemStack getItemStackOffHand();
    @Accessor
    void setItemStackOffHand(ItemStack value);
    @Accessor
    float getEquippedProgressMainHand();
    @Accessor
    void setEquippedProgressMainHand(float value);
    @Accessor
    float getPrevEquippedProgressMainHand();
    @Accessor
    void setPrevEquippedProgressMainHand(float value);
    @Accessor
    float getEquippedProgressOffHand();
    @Accessor
    void setEquippedProgressOffHand(float value);
    @Accessor
    float getPrevEquippedProgressOffHand();
    @Accessor
    void setPrevEquippedProgressOffHand(float value);
}
