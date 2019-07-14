package de.johni0702.minecraft.betterportals.impl;

import net.minecraftforge.common.config.Config;

public class PortalConfig {
    @Config.RequiresMcRestart
    @Config.Name("Enable")
    @Config.Comment("Whether to replace this kind of portal with BetterPortals ones.")
    public boolean enabled = true;

    @Config.Name("Opacity")
    @Config.Comment("Determines the opacity of the original portal texture.\n" +
            "A value of 0 will not render the original texture at all.\n" +
            "A value of 1 is maximally opaque, i.e. unchanged from the original value (the remote world will nevertheless be rendered).")
    @Config.RangeDouble(min = 0, max = 1)
    public double opacity = 0.5;
}
