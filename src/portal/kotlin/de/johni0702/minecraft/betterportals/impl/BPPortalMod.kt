package de.johni0702.minecraft.betterportals.impl

import de.johni0702.minecraft.betterportals.impl.client.audio.PortalAwareSoundManager
import de.johni0702.minecraft.betterportals.impl.client.renderer.PortalRenderManager
import de.johni0702.minecraft.betterportals.impl.common.maxRenderRecursionGetter
import de.johni0702.minecraft.betterportals.impl.common.preventFallDamageGetter
import de.johni0702.minecraft.betterportals.impl.net.Net

//#if MC>=11400
//#else
import net.minecraftforge.common.ForgeChunkManager
//#endif

//#if FABRIC>=1
//#else
import net.minecraftforge.fml.common.Mod
//#endif

//#if FABRIC<1
//#if MC>=11400
//$$ @Mod(BPPortalMod.MOD_ID)
//#else
@Mod(modid = BPPortalMod.MOD_ID, useMetadata = true)
//#endif
//#endif
class BPPortalMod : ModBase() {
    companion object {
        const val MOD_ID = "betterportals-portal"
    }

    init {
        theImpl = TheImpl
    }

    override fun commonInit() {
        preventFallDamageGetter = { BPConfig.preventFallDamage }
        maxRenderRecursionGetter = { if (BPConfig.seeThroughPortals) BPConfig.recursionLimit else 0 }

        Net.INSTANCE // initialize via <init>

        //#if MC<11400
        // Tickets are only allocated temporarily during remote portal frame search and otherwise aren't needed
        ForgeChunkManager.setForcedChunkLoadingCallback(this) { tickets, _ ->
            tickets.forEach { ForgeChunkManager.releaseTicket(it) }
        }
        //#endif
    }

    override fun clientInit() {
        PortalAwareSoundManager.dropRemoteSounds = { BPConfig.soundThroughPortals }
        PortalRenderManager.registered = true
    }
}