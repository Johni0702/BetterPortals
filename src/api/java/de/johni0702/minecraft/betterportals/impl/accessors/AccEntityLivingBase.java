package de.johni0702.minecraft.betterportals.impl.accessors;

import net.minecraft.entity.EntityLivingBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(EntityLivingBase.class)
public interface AccEntityLivingBase {
    //#if MC>=11400
    //$$ @Accessor
    //$$ int getNewPosRotationIncrements();
    //$$ @Accessor
    //$$ void setNewPosRotationIncrements(int value);
    //$$
    //$$ @Accessor
    //$$ double getInterpTargetX();
    //$$ @Accessor
    //$$ void setInterpTargetX(double value);
    //$$
    //$$ @Accessor
    //$$ double getInterpTargetY();
    //$$ @Accessor
    //$$ void setInterpTargetY(double value);
    //$$
    //$$ @Accessor
    //$$ double getInterpTargetZ();
    //$$ @Accessor
    //$$ void setInterpTargetZ(double value);
    //#endif
}
