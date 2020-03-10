package de.johni0702.minecraft.betterportals.impl.transition

import de.johni0702.minecraft.betterportals.impl.BPConfig
import de.johni0702.minecraft.betterportals.impl.ModBase
import de.johni0702.minecraft.betterportals.impl.transition.client.renderer.TransferToDimensionRenderer
import de.johni0702.minecraft.betterportals.impl.transition.net.Net
import de.johni0702.minecraft.betterportals.impl.transition.server.DimensionTransitionHandler
import java.time.Duration

//#if FABRIC>=1
//#else
import net.minecraftforge.fml.common.Mod
//#endif

//#if FABRIC<1
//#if MC>=11400
//$$ @Mod(BPTransitionMod.MOD_ID)
//#else
@Mod(modid = BPTransitionMod.MOD_ID, useMetadata = true)
//#endif
//#endif
class BPTransitionMod : ModBase() {
    companion object {
        const val MOD_ID = "betterportals-transition"
    }

    override fun commonInit() {
        Net.INSTANCE // initialize via <init>

        DimensionTransitionHandler.enabled = BPConfig.enhanceThirdPartyTransfers
        TransferToDimensionRenderer.defaultDuration = { Duration.ofSeconds(BPConfig.enhancedThirdPartyTransferSeconds.toLong()) }
    }
}
