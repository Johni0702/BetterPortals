package de.johni0702.minecraft.betterportals.impl.accessors;

import net.minecraft.client.entity.EntityOtherPlayerMP;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(EntityOtherPlayerMP.class)
public interface AccEntityOtherPlayerMP {
    //#if MC<11400
    @Accessor
    int getOtherPlayerMPPosRotationIncrements();
    @Accessor
    void setOtherPlayerMPPosRotationIncrements(int value);

    @Accessor
    double getOtherPlayerMPX();
    @Accessor
    void setOtherPlayerMPX(double value);

    @Accessor
    double getOtherPlayerMPY();
    @Accessor
    void setOtherPlayerMPY(double value);

    @Accessor
    double getOtherPlayerMPZ();
    @Accessor
    void setOtherPlayerMPZ(double value);

    @Accessor
    double getOtherPlayerMPYaw();
    @Accessor
    void setOtherPlayerMPYaw(double value);

    @Accessor
    double getOtherPlayerMPPitch();
    @Accessor
    void setOtherPlayerMPPitch(double value);
    //#endif
}
