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

    @Config.Name("TwilightForest Portals")
    @Config.Comment("Configuration for TwilightForest portals.")
    public static PortalConfig twilightForestPortals = new PortalConfig();

    @Config.Name("Mekanism Teleporter")
    @Config.Comment("Configuration for Mekanism Teleporter portals.")
    public static PortalConfig mekanismPortals = new PortalConfig();

    @Config.Name("Prevent Fall Damage")
    @Config.Comment("Whether to prevent the next fall damage after a player has passed through a lying portal.")
    public static boolean preventFallDamage = true;

    @Config.Name("See-through portals")
    @Config.Comment("Whether the other side of portals will be visible. Disabling will improve performance.")
    public static boolean seeThroughPortals = true;

    @SubscribeEvent
    public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
        if (MOD_ID.equals(event.getModID())) {
            ConfigManager.sync(MOD_ID, Config.Type.INSTANCE);
        }
    }
}
