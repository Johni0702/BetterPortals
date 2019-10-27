package de.johni0702.minecraft.betterportals.impl.accessors;

import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Entity.class)
public interface AccEntity {
    @Accessor
    Entity getRidingEntity();
    @Accessor
    void setRidingEntity(Entity value);
}
