package de.johni0702.minecraft.betterportals

import net.minecraftforge.common.config.Config

// TODO need some postprocessing in gradle build to annotate Kotlin's INSTANCE field with @Config.Ignore
@Config(modid = MOD_ID)
object BPConfig {
    @JvmField
    @Config.RequiresMcRestart
    @Config.Name("Enable Nether Portals")
    @Config.Comment("Whether to replace vanilla nether portals with BetterPortals ones.")
    var enableNetherPortals: Boolean = true

    @JvmField
    @Config.RequiresMcRestart
    @Config.Name("Enable End Portals")
    @Config.Comment("Whether to replace vanilla end portals with BetterPortals ones.")
    var enableEndPortals: Boolean = true

    @JvmField
    @Config.Name("Prevent Fall Damage")
    @Config.Comment("Whether to prevent the next fall damage after a player has passed through a lying portal.")
    var preventFallDamage: Boolean = true

    @JvmField
    @Config.RequiresMcRestart
    @Config.Name("Enable TwilightForest Portals (Experimental!)")
    @Config.Comment("Whether to replace TwilightForest portals with BetterPortals ones. Experimental feature!")
    var enableExperimentalTwilightForestPortals: Boolean = false

    @JvmField
    @Config.RequiresMcRestart
    @Config.Name("Replace Vanilla Chunk Render Dispatcher")
    @Config.Comment("Whether to replace the vanilla ChunkRenderDispatcher(s) with a view-aware one. For debugging only.")
    var improvedChunkRenderDispatcher: Boolean = true

    @JvmField
    @Config.Name("See-through portals")
    @Config.Comment("Whether the other side of portals will be visible. Disabling will improve performance.")
    var seeThroughPortals: Boolean = true
}