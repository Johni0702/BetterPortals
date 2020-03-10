package de.johni0702.minecraft.view.impl

import de.johni0702.minecraft.betterportals.impl.BPConfig
import de.johni0702.minecraft.betterportals.impl.ModBase
import de.johni0702.minecraft.view.common.viewApiImpl
import de.johni0702.minecraft.view.impl.client.render.ViewRenderManager
import de.johni0702.minecraft.view.impl.net.Net

//#if FABRIC>=1
//#else
import net.minecraftforge.fml.common.Mod
//#endif

//#if FABRIC<1
//#if MC>=11400
//$$ @Mod(BPViewAPIMod.MOD_ID)
//#else
@Mod(modid = BPViewAPIMod.MOD_ID, useMetadata = true)
//#endif
//#endif
class BPViewAPIMod : ModBase() {
    companion object {
        const val MOD_ID = "betterportals-view"
    }

    override fun commonInit() {
        Net.INSTANCE // initialize via <init>
        viewApiImpl = ViewAPIImpl
    }

    override fun clientInit() {
        ViewRenderManager.INSTANCE.debugView = { BPConfig.debugView }
        ClientViewAPIImpl.init()
    }
}