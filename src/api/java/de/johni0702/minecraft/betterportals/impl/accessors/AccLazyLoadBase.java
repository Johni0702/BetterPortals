package de.johni0702.minecraft.betterportals.impl.accessors;

import net.minecraft.util.LazyLoadBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

//#if MC>=11400
//$$ import java.util.function.Supplier;
//#endif

@Mixin(LazyLoadBase.class)
public interface AccLazyLoadBase {
    @Accessor
    //#if MC>=11400
    //$$ Supplier<?> getSupplier();
    //#else
    boolean getIsLoaded();
    //#endif
}
