package de.johni0702.minecraft.betterportals;

import net.minecraftforge.common.config.Config;

import static de.johni0702.minecraft.betterportals.BetterPortalsModKt.MOD_ID;

@Config(modid = MOD_ID)
public class BPConfig {
    @Config.RequiresMcRestart
    @Config.Name("Enable Nether Portals")
    @Config.Comment("Whether to replace vanilla nether portals with BetterPortals ones.")
    public static boolean enableNetherPortals = true;

    @Config.RequiresMcRestart
    @Config.Name("Enable End Portals")
    @Config.Comment("Whether to replace vanilla end portals with BetterPortals ones.")
    public static boolean enableEndPortals = true;

    @Config.Name("Prevent Fall Damage")
    @Config.Comment("Whether to prevent the next fall damage after a player has passed through a lying portal.")
    public static boolean preventFallDamage = true;

    @Config.RequiresMcRestart
    @Config.Name("Enable TwilightForest Portals (Experimental!)")
    @Config.Comment("Whether to replace TwilightForest portals with BetterPortals ones. Experimental feature!")
    public static boolean enableExperimentalTwilightForestPortals = false;

    @Config.RequiresMcRestart
    @Config.Name("Replace Vanilla Chunk Render Dispatcher")
    @Config.Comment("Whether to replace the vanilla ChunkRenderDispatcher(s) with a view-aware one. For debugging only.")
    public static boolean improvedChunkRenderDispatcher = true;

    @Config.Name("See-through portals")
    @Config.Comment("Whether the other side of portals will be visible. Disabling will improve performance.")
    public static boolean seeThroughPortals = true;
}
