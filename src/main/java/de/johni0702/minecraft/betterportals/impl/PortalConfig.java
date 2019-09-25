package de.johni0702.minecraft.betterportals.impl;

import net.minecraftforge.common.config.Config;

public class PortalConfig {
    @Config.RequiresMcRestart
    @Config.Name("Enable")
    @Config.Comment("Whether to replace this kind of portal with BetterPortals ones. Needs to be set on the server and the client.")
    public boolean enabled = true;

    @Config.Name("Opacity")
    @Config.Comment("Determines the opacity of the original portal texture.\n" +
            "A value of 0 will not render the original texture at all.\n" +
            "A value of 1 is maximally opaque, i.e. unchanged from the original value (the remote world will nevertheless be rendered).\n" +
            "\n" +
            "Client-side setting.\n")
    @Config.RangeDouble(min = 0, max = 1)
    public double opacity = 0.5;

    @Config.Name("Fully visible distance")
    @Config.Comment("The distance at which portals will fully render the remote world.\n" +
            "If this value is greater than the \"Minimal render distance\", that value is used instead.\n" +
            "\n" +
            "A value between 0 and 1 specifies a fraction of the overall render distance.\n" +
            "A value equal or greater than 1 specifies an absolute distance in chunks.\n" +
            "\n" +
            "Client-side setting.\n"
    )
    @Config.RangeDouble(min = 0, max = 64)
    public double renderDistMin = 1.0;

    @Config.Name("Minimal render distance")
    @Config.Comment("The distance at which portals will begin to render the remote world.\n" +
            "\n" +
            "A value between 0 and 1 specifies a fraction of the overall render distance.\n" +
            "A value equal or greater than 1 specifies an absolute distance in chunks.\n" +
            "\n" +
            "Client-side setting.\n"
    )
    @Config.RangeDouble(min = 0, max = 64)
    public double renderDistMax = 3.00;

    @Config.Name("Size render distance multiplier")
    @Config.Comment("For portals which have a side length greater than this value, the \"Minimal render distance\" and \"Fully visible distance\" values\n will be doubled.\n" +
            "If the side length is greater than twice this value, they will be tripled.\n" +
            "Greater than thrice this value, they will be quadrupled.\n" +
            "Etc.\n" +
            "\n" +
            "Render dist values between 0 and 1 will be scaled after they've been converted into absolute ones.\n" +
            "\n" +
            "Client-side setting.\n"
    )
    public int renderDistSizeMultiplier = 10;
}
