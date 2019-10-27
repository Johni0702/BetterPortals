package de.johni0702.minecraft.betterportals.impl.accessors;

import net.minecraft.entity.player.EntityPlayerMP;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(EntityPlayerMP.class)
public interface AccEntityPlayerMP {
    @Accessor
    List<Integer> getEntityRemoveQueue();
}
