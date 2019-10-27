package de.johni0702.minecraft.betterportals.impl.accessors;

import net.minecraft.entity.EntityLivingBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(EntityLivingBase.class)
public interface AccAbstractClientPlayer {
    @Accessor
    int getTicksElytraFlying();
    @Accessor
    void setTicksElytraFlying(int value);
}
