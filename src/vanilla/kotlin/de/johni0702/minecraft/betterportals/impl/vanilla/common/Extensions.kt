package de.johni0702.minecraft.betterportals.impl.vanilla.common

import de.johni0702.minecraft.betterportals.client.render.FramedPortalRenderer
import de.johni0702.minecraft.betterportals.client.render.RenderOneWayPortalEntity
import de.johni0702.minecraft.betterportals.client.render.RenderPortalEntity
import de.johni0702.minecraft.betterportals.common.PortalConfiguration
import de.johni0702.minecraft.betterportals.common.entity.PortalEntityAccessor
import de.johni0702.minecraft.betterportals.common.portalManager
import de.johni0702.minecraft.betterportals.impl.EntityTypeRegistry
import de.johni0702.minecraft.betterportals.impl.TileEntityTypeRegistry
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
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.client.registry.ClientRegistry
import net.minecraftforge.fml.client.registry.RenderingRegistry
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.registries.IForgeRegistry

//#if MC>=11400
//$$ import net.minecraft.entity.EntityClassification
//$$ import net.minecraft.entity.EntityType
//$$ import net.minecraft.world.World
//$$ import com.mojang.datafixers.DataFixUtils
//$$ import net.minecraft.block.Blocks
//$$ import net.minecraft.tileentity.TileEntityType
//$$ import net.minecraft.util.SharedConstants
//$$ import net.minecraft.util.datafix.DataFixesManager
//$$ import net.minecraft.util.datafix.TypeReferences
//$$ import java.util.function.Supplier
//#else
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.ResourceLocation
import net.minecraftforge.fml.common.registry.EntityRegistry
//#endif

const val MOD_ID = "betterportals"
internal lateinit var NETHER_PORTAL_CONFIG: PortalConfiguration
internal lateinit var END_PORTAL_CONFIG: PortalConfiguration

fun initVanilla(
        mod: Any,
        clientPreInit: (() -> Unit) -> Unit,
        registerBlocks: (IForgeRegistry<Block>.() -> Unit) -> Unit,
        registerTileEntities: (TileEntityTypeRegistry.() -> Unit) -> Unit,
        registerEntities: (EntityTypeRegistry.() -> Unit) -> Unit,
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
        }
    }

    registerTileEntities {
        if (enableEndPortals) {
            //#if MC>=11400
            //$$ val key = "end_portal"
            //$$ val dataFixerType = DataFixesManager.getDataFixer()
            //$$         .getSchema(DataFixUtils.makeKey(SharedConstants.getVersion().worldVersion))
            //$$         .getChoiceType(TypeReferences.BLOCK_ENTITY, key)
            //$$
            //$$ TileEntityType.Builder.create(Supplier { TileEntityBetterEndPortal() }, Blocks.END_PORTAL)
            //$$         .build(dataFixerType)
            //$$         .also { register(it) }
            //#else
            TileEntity.register("end_portal", TileEntityBetterEndPortal::class.java)
            //#endif
        }
    }

    registerEntities {
        if (enableNetherPortals) {
            //#if MC>=11400
            //$$ EntityType.Builder.create<NetherPortalEntity>(::NetherPortalEntity, EntityClassification.MISC)
            //$$         .disableSummoning()
            //$$         .immuneToFire()
            //$$         .size(0f, 0f)
            //$$         .setUpdateInterval(Int.MAX_VALUE)
            //$$         .setTrackingRange(256)
            //$$         .setCustomClientFactory { _, world -> NetherPortalEntity(world = world) }
            //$$         .build("$MOD_ID:nether_portal")
            //$$         .also { NetherPortalEntity.ENTITY_TYPE = it }
            //#else
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
            //#endif
            MinecraftForge.EVENT_BUS.register(object {
                @SubscribeEvent
                fun onWorld(event: WorldEvent.Load) {
                    val world = event.world
                    //#if MC>=11400
                    //$$ if (world !is World) return
                    //#endif
                    world.portalManager.registerPortals(PortalEntityAccessor(NetherPortalEntity::class.java, world))
                }
            })
        }
        if (enableEndPortals) {
            //#if MC>=11400
            //$$ EntityType.Builder.create<EndEntryPortalEntity>(::EndEntryPortalEntity, EntityClassification.MISC)
            //$$         .disableSummoning()
            //$$         .immuneToFire()
            //$$         .size(0f, 0f)
            //$$         .setUpdateInterval(Int.MAX_VALUE)
            //$$         .setTrackingRange(256)
            //$$         .setCustomClientFactory { _, world -> EndEntryPortalEntity(world = world) }
            //$$         .build("$MOD_ID:end_entry_portal")
            //$$         .also { EndEntryPortalEntity.ENTITY_TYPE = it }
            //$$ EntityType.Builder.create<EndExitPortalEntity>(::EndExitPortalEntity, EntityClassification.MISC)
            //$$         .disableSummoning()
            //$$         .immuneToFire()
            //$$         .size(0f, 0f)
            //$$         .setUpdateInterval(Int.MAX_VALUE)
            //$$         .setTrackingRange(256)
            //$$         .setCustomClientFactory { _, world -> EndExitPortalEntity(world = world) }
            //$$         .build("$MOD_ID:end_exit_portal")
            //$$         .also { EndExitPortalEntity.ENTITY_TYPE = it }
            //#else
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
            //#endif
            MinecraftForge.EVENT_BUS.register(object {
                @SubscribeEvent
                fun onWorld(event: WorldEvent.Load) {
                    val world = event.world
                    //#if MC>=11400
                    //$$ if (world !is World) return
                    //#endif
                    val portalManager = world.portalManager
                    portalManager.registerPortals(PortalEntityAccessor(EndEntryPortalEntity::class.java, world))
                    portalManager.registerPortals(PortalEntityAccessor(EndExitPortalEntity::class.java, world))
                }
            })
        }
    }
}
