package de.johni0702.minecraft.betterportals.impl;

import net.minecraftforge.common.config.Config;

public class PortalConfig {
    @Config.RequiresMcRestart
    @Config.Name("Enable")
    @Config.Comment("Whether to replace this kind of portal with BetterPortals ones.")
    public boolean enabled = true;
}
