package de.johni0702.minecraft.betterportals.impl;

import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import static de.johni0702.minecraft.betterportals.impl.BetterPortalsModKt.MOD_ID;

@Config(modid = MOD_ID)
@Mod.EventBusSubscriber(modid = MOD_ID)
public class BPConfig {
    @Config.Name("Vanilla Nether Portals")
    @Config.Comment("Configuration for vanilla nether portals.")
    public static PortalConfig netherPortals = new PortalConfig();

    @Config.Name("Vanilla End Portals")
    @Config.Comment("Configuration for vanilla end portals")
    public static PortalConfig endPortals = new PortalConfig();
    static {
        // End entry portal is 40 blocks above the exit and we'd like to see it clearly.
        // Rounding up, that's three chunks.
        // These settings aren't as important for end portals anyway, because the entry portal is usually take care of
        // via occlusion culling and the exit portal is the only one around.
        endPortals.renderDistMin = 3;
        endPortals.renderDistMax = 6;
    }

    @Config.Name("TwilightForest Portals")
    @Config.Comment("Configuration for TwilightForest portals.")
    public static PortalConfig twilightForestPortals = new PortalConfig();

    @Config.Name("Mekanism Teleporter")
    @Config.Comment("Configuration for Mekanism Teleporter portals.")
    public static PortalConfig mekanismPortals = new PortalConfig();

    @Config.Name("The Aether Portals")
    @Config.Comment("Configuration for The Aether portals.")
    public static PortalConfig aetherPortals = new PortalConfig();

    @Config.Name("Prevent Fall Damage")
    @Config.Comment("Whether to prevent the next fall damage after a player has passed through a lying portal.")
    public static boolean preventFallDamage = true;

    @Config.Name("See-through portals")
    @Config.Comment("Whether the other side of portals will be visible. Disabling will improve performance.")
    public static boolean seeThroughPortals = true;

    @Config.Name("Portals in portals limit")
    @Config.Comment("How deeply nested portals are rendered.\n" +
            "A value of 0 will completely disable see-through portals.\n" +
            "A value of 1 will allow you to see through portals but not portals within those.\n" +
            "A value of 2 (default) will allow you to see through portals in portals but no further.\n" +
            "A value of 3 will allow you to see through portals in portals in portals but no further.\n" +
            "Etc.\n" +
            "\n" +
            "This only applies to portals which already have their remote world loaded.\n" +
            "The recursion limit for loading of portals is a fixed 1.")
    @Config.RangeInt(min = 0)
    public static int recursionLimit = 2;

    @Config.Name("Enhance third-party transfers")
    @Config.Comment("BetterPortals can replace the loading screen on transfer to a different world when triggered by a" +
            "third-party mod with a custom \"blobby-transition-thing\".\n" +
            "\n" +
            "Disable (and report on the issue tracker) in case of compatibility issues.\n" +
            "\n" +
            "Needs to be set on the server / has no effect on the client.")
    @Config.RequiresMcRestart
    public static boolean enhanceThirdPartyTransfers = true;

    @Config.Name("Debug View")
    @Config.Comment("Show debug view instead of composed view.")
    public static boolean debugView = false;

    @SubscribeEvent
    public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
        if (MOD_ID.equals(event.getModID())) {
            ConfigManager.sync(MOD_ID, Config.Type.INSTANCE);
        }
    }
}
