package de.johni0702.minecraft.betterportals.impl.accessors;

import net.minecraft.entity.item.EntityMinecart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(EntityMinecart.class)
public interface AccEntityMinecart {
    @Accessor
    int getTurnProgress();
    @Accessor
    void setTurnProgress(int value);

    @Accessor
    double getMinecartX();
    @Accessor
    void setMinecartX(double value);

    @Accessor
    double getMinecartY();
    @Accessor
    void setMinecartY(double value);

    @Accessor
    double getMinecartZ();
    @Accessor
    void setMinecartZ(double value);

    @Accessor
    double getMinecartYaw();
    @Accessor
    void setMinecartYaw(double value);

    @Accessor
    double getMinecartPitch();
    @Accessor
    void setMinecartPitch(double value);
}
