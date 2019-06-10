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

    @NotNull
    @Override
    public PortalManager getPortalManager() {
        return portalManager;
    }
}
