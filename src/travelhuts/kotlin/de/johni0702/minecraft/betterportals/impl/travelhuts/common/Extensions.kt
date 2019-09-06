//#if MC<11400
package de.johni0702.minecraft.betterportals.impl.travelhuts.common

import de.johni0702.minecraft.betterportals.client.render.RenderPortalEntity
import de.johni0702.minecraft.betterportals.common.PortalConfiguration
import de.johni0702.minecraft.betterportals.common.entity.PortalEntityAccessor
import de.johni0702.minecraft.betterportals.common.portalManager
import de.johni0702.minecraft.betterportals.impl.travelhuts.client.renderer.TravelHutsPortalRenderer
import de.johni0702.minecraft.betterportals.impl.travelhuts.common.blocks.BlockBetterTravelHutsPortal
import de.johni0702.minecraft.betterportals.impl.travelhuts.common.entity.TravelHutsPortalEntity
import info.loenwind.travelhut.TravelHutMod
import net.minecraft.block.Block
import net.minecraft.util.ResourceLocation
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.client.registry.RenderingRegistry
import net.minecraftforge.fml.common.Loader
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.registry.EntityRegistry
import net.minecraftforge.registries.IForgeRegistry

const val MOD_ID = "betterportals"
const val TRAVELHUTS_MOD_ID = "travelhut"

lateinit var TRAVELHUTS_PORTAL_CONFIG: PortalConfiguration

private val hasTravelHuts by lazy { Loader.isModLoaded(TRAVELHUTS_MOD_ID) }

fun initTravelHuts(
        mod: Any,
        clientPreInit: (() -> Unit) -> Unit,
        init: (() -> Unit) -> Unit,
        registerBlocks: (IForgeRegistry<Block>.() -> Unit) -> Unit,
        enableTravelHutsPortals: Boolean,
        configTravelHutsPortals: PortalConfiguration
) {
    TRAVELHUTS_PORTAL_CONFIG = configTravelHutsPortals

    if (!enableTravelHutsPortals || !hasTravelHuts) {
        return
    }

    clientPreInit {
        RenderingRegistry.registerEntityRenderingHandler(TravelHutsPortalEntity::class.java) {
            RenderPortalEntity(it, TravelHutsPortalRenderer(configTravelHutsPortals.opacity))
        }
    }

    registerBlocks {
        register(BlockBetterTravelHutsPortal().also { TravelHutMod.blockHutPortal = it })
    }

    init {
        EntityRegistry.registerModEntity(
                ResourceLocation(MOD_ID, "travelhuts_portal"),
                TravelHutsPortalEntity::class.java,
                "travelhuts_portal",
                8,
                mod,
                256,
                Int.MAX_VALUE,
                false
        )
        MinecraftForge.EVENT_BUS.register(object {
            @SubscribeEvent
            fun onWorld(event: WorldEvent.Load) {
                val world = event.world
                world.portalManager.registerPortals(PortalEntityAccessor(TravelHutsPortalEntity::class.java, world))
            }
        })
    }
}
//#endif
