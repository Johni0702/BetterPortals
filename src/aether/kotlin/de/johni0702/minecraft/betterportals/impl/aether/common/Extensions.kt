//#if MC<11400
package de.johni0702.minecraft.betterportals.impl.aether.common

import com.legacy.aether.blocks.BlocksAether
import de.johni0702.minecraft.betterportals.client.render.FramedPortalRenderer
import de.johni0702.minecraft.betterportals.client.render.RenderPortalEntity
import de.johni0702.minecraft.betterportals.common.PortalConfiguration
import de.johni0702.minecraft.betterportals.common.entity.PortalEntityAccessor
import de.johni0702.minecraft.betterportals.common.portalManager
import de.johni0702.minecraft.betterportals.impl.aether.common.blocks.BlockBetterAetherPortal
import de.johni0702.minecraft.betterportals.impl.aether.common.entity.AetherPortalEntity
import net.minecraft.block.Block
import net.minecraft.client.Minecraft
import net.minecraft.util.ResourceLocation
import net.minecraft.util.math.AxisAlignedBB
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.client.registry.RenderingRegistry
import net.minecraftforge.fml.common.Loader
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.registry.EntityRegistry
import net.minecraftforge.registries.IForgeRegistry

internal val EMPTY_AABB = AxisAlignedBB(0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
const val MOD_ID = "betterportals"
const val AETHER_MOD_ID = "aether_legacy"

lateinit var AETHER_PORTAL_CONFIG: PortalConfiguration

private val hasAether by lazy { Loader.isModLoaded(AETHER_MOD_ID) }

fun initAether(
        mod: Any,
        clientPreInit: (() -> Unit) -> Unit,
        init: (() -> Unit) -> Unit,
        registerBlocks: (IForgeRegistry<Block>.() -> Unit) -> Unit,
        enableAetherPortals: Boolean,
        configAetherPortals: PortalConfiguration
) {
    AETHER_PORTAL_CONFIG = configAetherPortals

    if (!enableAetherPortals || !hasAether) {
        return
    }

    clientPreInit {
        RenderingRegistry.registerEntityRenderingHandler(AetherPortalEntity::class.java) {
            RenderPortalEntity(it, FramedPortalRenderer(configAetherPortals.opacity, {
                Minecraft.getMinecraft().textureMapBlocks.getAtlasSprite("$AETHER_MOD_ID:blocks/aether_portal")
            }))
        }
    }

    registerBlocks {
        register(BlockBetterAetherPortal(mod).also { BlocksAether.aether_portal = it })
    }

    init {
        EntityRegistry.registerModEntity(
                ResourceLocation(MOD_ID, "aether_portal"),
                AetherPortalEntity::class.java,
                "aether_portal",
                4,
                mod,
                256,
                Int.MAX_VALUE,
                false
        )
        MinecraftForge.EVENT_BUS.register(object {
            @SubscribeEvent
            fun onWorld(event: WorldEvent.Load) {
                val world = event.world
                world.portalManager.registerPortals(PortalEntityAccessor(AetherPortalEntity::class.java, world))
            }
        })
    }
}
//#endif
