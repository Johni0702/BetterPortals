package de.johni0702.minecraft.betterportals.impl.mixin;

import de.johni0702.minecraft.betterportals.common.PortalManager;
import de.johni0702.minecraft.betterportals.impl.common.HasPortalManager;
import de.johni0702.minecraft.betterportals.impl.common.PortalManagerImpl;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(World.class)
public abstract class MixinWorld implements HasPortalManager {
    private PortalManager portalManager = new PortalManagerImpl((World) (Object) this);
    //#if FABRIC>=1
    //$$ private boolean portalAccessorsRegistered;
    //#endif

    @NotNull
    @Override
    public PortalManager getPortalManager() {
        //#if FABRIC>=1
        //$$ if (!portalAccessorsRegistered) {
        //$$     PortalManager.REGISTER_ACCESSORS_EVENT.invoker().handle(portalManager);
        //$$     portalAccessorsRegistered = true;
        //$$ }
        //#endif
        return portalManager;
    }
}
