//#if MC<11400
package de.johni0702.minecraft.betterportals.impl.travelhuts

import de.johni0702.minecraft.betterportals.client.render.RenderPortalEntity
import de.johni0702.minecraft.betterportals.common.entity.PortalEntityAccessor
import de.johni0702.minecraft.betterportals.impl.BPConfig
import de.johni0702.minecraft.betterportals.impl.BlockRegistry
import de.johni0702.minecraft.betterportals.impl.EntityTypeRegistry
import de.johni0702.minecraft.betterportals.impl.ModBase
import de.johni0702.minecraft.betterportals.impl.registerPortalAccessor
import de.johni0702.minecraft.betterportals.impl.toConfiguration
import de.johni0702.minecraft.betterportals.impl.travelhuts.client.renderer.TravelHutsPortalRenderer
import de.johni0702.minecraft.betterportals.impl.travelhuts.common.TRAVELHUTS_MOD_ID
import de.johni0702.minecraft.betterportals.impl.travelhuts.common.blocks.BlockBetterTravelHutsPortal
import de.johni0702.minecraft.betterportals.impl.travelhuts.common.entity.TravelHutsPortalEntity
import info.loenwind.travelhut.TravelHutMod
import net.minecraft.util.ResourceLocation
import net.minecraftforge.fml.client.registry.RenderingRegistry
import net.minecraftforge.fml.common.Loader
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.registry.EntityRegistry

@Mod(modid = BPTravelHutsMod.MOD_ID, useMetadata = true)
class BPTravelHutsMod : ModBase() {
    companion object {
        const val MOD_ID = "betterportals-travelhuts"
        internal val PORTAL_CONFIG = BPConfig.travelHutsPortals.toConfiguration()
    }

    override val canLoad: Boolean by lazy { Loader.isModLoaded(TRAVELHUTS_MOD_ID) }

    override fun BlockRegistry.registerBlocks() {
        register(BlockBetterTravelHutsPortal().also { TravelHutMod.blockHutPortal = it })
    }

    override fun EntityTypeRegistry.registerEntities() {
        EntityRegistry.registerModEntity(
                ResourceLocation("betterportals", "travelhuts_portal"),
                TravelHutsPortalEntity::class.java,
                "travelhuts_portal",
                8,
                this@BPTravelHutsMod,
                256,
                Int.MAX_VALUE,
                false
        )
        registerPortalAccessor { PortalEntityAccessor(TravelHutsPortalEntity::class.java, it) }
    }

    override fun clientPreInit() {
        RenderingRegistry.registerEntityRenderingHandler(TravelHutsPortalEntity::class.java) {
            RenderPortalEntity(it, TravelHutsPortalRenderer(PORTAL_CONFIG.opacity))
        }
    }
}
//#endif