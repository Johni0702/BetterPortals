//#if MC<11400
package de.johni0702.minecraft.betterportals.impl.accessors;

import net.minecraft.entity.EntityTracker;
import net.minecraft.entity.EntityTrackerEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Set;

@Mixin(EntityTracker.class)
public interface AccEntityTracker {
    @Accessor
    Set<EntityTrackerEntry> getEntries();
}
//#endif
