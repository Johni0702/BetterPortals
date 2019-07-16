package de.johni0702.minecraft.betterportals.impl.vanilla.common

import de.johni0702.minecraft.betterportals.client.render.FramedPortalRenderer
import de.johni0702.minecraft.betterportals.client.render.RenderOneWayPortalEntity
import de.johni0702.minecraft.betterportals.client.render.RenderPortalEntity
import de.johni0702.minecraft.betterportals.common.PortalConfiguration
import de.johni0702.minecraft.betterportals.common.entity.PortalEntityAccessor
import de.johni0702.minecraft.betterportals.common.portalManager
import de.johni0702.minecraft.betterportals.impl.vanilla.client.renderer.EndPortalRenderer
import de.johni0702.minecraft.betterportals.impl.vanilla.client.tile.renderer.BetterEndPortalTileRenderer
import de.johni0702.minecraft.betterportals.impl.vanilla.common.blocks.BlockBetterEndPortal
import de.johni0702.minecraft.betterportals.impl.vanilla.common.blocks.BlockBetterNetherPortal
import de.johni0702.minecraft.betterportals.impl.vanilla.common.blocks.TileEntityBetterEndPortal
import de.johni0702.minecraft.betterportals.impl.vanilla.common.entity.EndEntryPortalEntity
import de.johni0702.minecraft.betterportals.impl.vanilla.common.entity.EndExitPortalEntity
import de.johni0702.minecraft.betterportals.impl.vanilla.common.entity.NetherPortalEntity
import net.minecraft.block.Block
import net.minecraft.client.Minecraft
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.ResourceLocation
import net.minecraft.util.math.AxisAlignedBB
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.client.registry.ClientRegistry
import net.minecraftforge.fml.client.registry.RenderingRegistry
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.registry.EntityRegistry
import net.minecraftforge.registries.IForgeRegistry

const val MOD_ID = "betterportals"
internal val EMPTY_AABB = AxisAlignedBB(0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
internal lateinit var NETHER_PORTAL_CONFIG: PortalConfiguration
internal lateinit var END_PORTAL_CONFIG: PortalConfiguration

fun initVanilla(
        mod: Any,
        clientPreInit: (() -> Unit) -> Unit,
        init: (() -> Unit) -> Unit,
        registerBlocks: (IForgeRegistry<Block>.() -> Unit) -> Unit,
        enableNetherPortals: Boolean,
        enableEndPortals: Boolean,
        configNetherPortals: PortalConfiguration,
        configEndPortals: PortalConfiguration
) {
    NETHER_PORTAL_CONFIG = configNetherPortals
    END_PORTAL_CONFIG = configEndPortals

    clientPreInit {
        if (enableNetherPortals) {
            RenderingRegistry.registerEntityRenderingHandler(NetherPortalEntity::class.java) {
                RenderPortalEntity(it, FramedPortalRenderer(configNetherPortals.opacity, {
                    Minecraft.getMinecraft().textureMapBlocks.getAtlasSprite("minecraft:blocks/portal")
                }))
            }
        }
        if (enableEndPortals) {
            ClientRegistry.bindTileEntitySpecialRenderer(TileEntityBetterEndPortal::class.java, BetterEndPortalTileRenderer())
            RenderingRegistry.registerEntityRenderingHandler(EndEntryPortalEntity::class.java) {
                RenderOneWayPortalEntity(it, EndPortalRenderer(configEndPortals.opacity))
            }
            RenderingRegistry.registerEntityRenderingHandler(EndExitPortalEntity::class.java) {
                RenderOneWayPortalEntity(it, EndPortalRenderer(configEndPortals.opacity))
            }
        }
    }

    registerBlocks {
        if (enableNetherPortals) register(BlockBetterNetherPortal(mod))
        if (enableEndPortals) {
            register(BlockBetterEndPortal())
            TileEntity.register("end_portal", TileEntityBetterEndPortal::class.java)
        }
    }

    init {
        if (enableNetherPortals) {
            EntityRegistry.registerModEntity(
                    ResourceLocation(MOD_ID, "nether_portal"),
                    NetherPortalEntity::class.java,
                    "nether_portal",
                    0,
                    mod,
                    256,
                    Int.MAX_VALUE,
                    false
            )
            MinecraftForge.EVENT_BUS.register(object {
                @SubscribeEvent
                fun onWorld(event: WorldEvent.Load) {
                    val world = event.world
                    event.world.portalManager.registerPortals(PortalEntityAccessor(NetherPortalEntity::class.java, world))
                }
            })
        }
        if (enableEndPortals) {
            EntityRegistry.registerModEntity(
                    ResourceLocation(MOD_ID, "end_entry_portal"),
                    EndEntryPortalEntity::class.java,
                    "end_entry_portal",
                    1,
                    mod,
                    256,
                    Int.MAX_VALUE,
                    false
            )
            EntityRegistry.registerModEntity(
                    ResourceLocation(MOD_ID, "end_exit_portal"),
                    EndExitPortalEntity::class.java,
                    "end_exit_portal",
                    2,
                    mod,
                    256,
                    Int.MAX_VALUE,
                    false
            )
            MinecraftForge.EVENT_BUS.register(object {
                @SubscribeEvent
                fun onWorld(event: WorldEvent.Load) {
                    val world = event.world
                    val portalManager = event.world.portalManager
                    portalManager.registerPortals(PortalEntityAccessor(EndEntryPortalEntity::class.java, world))
                    portalManager.registerPortals(PortalEntityAccessor(EndExitPortalEntity::class.java, world))
                }
            })
        }
    }
}
