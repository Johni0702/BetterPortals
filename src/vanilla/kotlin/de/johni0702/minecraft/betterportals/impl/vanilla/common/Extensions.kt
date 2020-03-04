package de.johni0702.minecraft.betterportals.impl.vanilla.common

import de.johni0702.minecraft.betterportals.client.render.FramedPortalRenderer
import de.johni0702.minecraft.betterportals.client.render.RenderOneWayPortalEntity
import de.johni0702.minecraft.betterportals.client.render.RenderPortalEntity
import de.johni0702.minecraft.betterportals.common.PortalConfiguration
import de.johni0702.minecraft.betterportals.common.entity.PortalEntityAccessor
import de.johni0702.minecraft.betterportals.impl.BlockRegistry
import de.johni0702.minecraft.betterportals.impl.register
import de.johni0702.minecraft.betterportals.impl.registerBlockEntityRenderer
import de.johni0702.minecraft.betterportals.impl.registerEntityRenderer
import de.johni0702.minecraft.betterportals.impl.registerPortalAccessor
import de.johni0702.minecraft.betterportals.impl.EntityTypeRegistry
import de.johni0702.minecraft.betterportals.impl.TileEntityTypeRegistry
import de.johni0702.minecraft.betterportals.impl.vanilla.client.renderer.EndPortalRenderer
import de.johni0702.minecraft.betterportals.impl.vanilla.client.tile.renderer.BetterEndPortalTileRenderer
import de.johni0702.minecraft.betterportals.impl.vanilla.common.blocks.TileEntityBetterEndPortal
import de.johni0702.minecraft.betterportals.impl.vanilla.common.entity.EndEntryPortalEntity
import de.johni0702.minecraft.betterportals.impl.vanilla.common.entity.EndExitPortalEntity
import de.johni0702.minecraft.betterportals.impl.vanilla.common.entity.NetherPortalEntity
import net.minecraft.client.Minecraft
import net.minecraft.init.Blocks
import net.minecraft.util.ResourceLocation

//#if FABRIC>=1
//$$ import net.fabricmc.fabric.api.event.client.ClientSpriteRegistryCallback
//$$ import net.minecraft.client.texture.SpriteAtlasTexture
//$$ import net.minecraft.entity.EntityDimensions
//#endif

//#if MC>=11400
//$$ import net.minecraft.entity.EntityClassification
//$$ import com.mojang.datafixers.DataFixUtils
//$$ import de.johni0702.minecraft.betterportals.impl.registerEntityType
//$$ import net.minecraft.tileentity.TileEntityType
//$$ import net.minecraft.util.SharedConstants
//$$ import net.minecraft.util.datafix.DataFixesManager
//$$ import net.minecraft.util.datafix.TypeReferences
//$$ import java.util.function.Supplier
//#else
import net.minecraft.tileentity.TileEntity
import net.minecraftforge.fml.common.Loader
import net.minecraftforge.fml.common.registry.EntityRegistry
//#endif

const val MOD_ID = "betterportals"
internal lateinit var NETHER_PORTAL_CONFIG: PortalConfiguration
internal lateinit var END_PORTAL_CONFIG: PortalConfiguration

interface BlockWithBPVersion {
    fun enableBetterVersion(mod: Any)
}

fun initVanilla(
        mod: Any,
        clientPreInit: (() -> Unit) -> Unit,
        postInit: (() -> Unit) -> Unit,
        registerBlocks: (BlockRegistry.() -> Unit) -> Unit,
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
            //#if MC>=11400
            //$$ val netherPortalSpriteId = ResourceLocation("minecraft", "block/nether_portal")
            //#endif
            registerEntityRenderer<NetherPortalEntity> {
                RenderPortalEntity(it, FramedPortalRenderer(configNetherPortals.opacity, {
                    //#if MC>=11400
                    //$$ Minecraft.getInstance().textureMap.getSprite(netherPortalSpriteId)
                    //#else
                    Minecraft.getMinecraft().textureMapBlocks.getAtlasSprite("minecraft:blocks/portal")
                    //#endif
                }))
            }
            // Note: This is only required on fabric because there we replace the vanilla portal block. Forge on the
            //       other hand has the "overwrite" concept, where it keeps both registrations (and thereby the vanilla
            //       block model registers the sprite for us).
            //#if FABRIC>=1
            //$$ ClientSpriteRegistryCallback.event(SpriteAtlasTexture.BLOCK_ATLAS_TEX).register(ClientSpriteRegistryCallback { _, registry ->
            //$$     registry.register(netherPortalSpriteId)
            //$$ })
            //#endif
        }
        if (enableEndPortals) {
            registerBlockEntityRenderer<TileEntityBetterEndPortal>(BetterEndPortalTileRenderer())
            registerEntityRenderer<EndEntryPortalEntity> {
                RenderOneWayPortalEntity(it, EndPortalRenderer(configEndPortals.opacity))
            }
            registerEntityRenderer<EndExitPortalEntity> {
                RenderOneWayPortalEntity(it, EndPortalRenderer(configEndPortals.opacity))
            }
        }
    }

    registerBlocks {
        //#if MC>=11400
        if (enableNetherPortals && Loader.isModLoaded("mekanism")) {
            // If we aren't doing it, Mekanism will (and break our stuff cause it overwrites important methods)
            register(object : net.minecraft.block.BlockPortal() {
                init {
                    registryName = ResourceLocation("minecraft", "portal")
                    setHardness(-1.0F)
                    soundType = net.minecraft.block.SoundType.GLASS
                    setLightLevel(0.75F)
                    unlocalizedName = "portal"
                }
            })
        }
        //#endif
    }

    postInit {
        if (enableNetherPortals) {
            (Blocks.PORTAL as BlockWithBPVersion).enableBetterVersion(mod)
        }
        if (enableEndPortals) {
            (Blocks.END_PORTAL as BlockWithBPVersion).enableBetterVersion(mod)
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
            //$$ val type = TileEntityType.Builder.create(Supplier { TileEntityBetterEndPortal() }, Blocks.END_PORTAL)
            //$$         .build(dataFixerType)
            //$$ TileEntityBetterEndPortal.TYPE = type
            //$$ register(ResourceLocation("minecraft", key), type)
            //#else
            TileEntity.register("end_portal", TileEntityBetterEndPortal::class.java)
            //#endif
        }
    }

    registerEntities {
        if (enableNetherPortals) {
            //#if MC>=11400
            //$$ registerEntityType(NetherPortalEntity.ID, ::NetherPortalEntity, EntityClassification.MISC) {
                //#if FABRIC>=1
                //$$ disableSummon()
                //$$ setImmuneToFire()
                //$$ size(EntityDimensions.fixed(0f, 0f))
                //$$ trackable(256, Int.MAX_VALUE)
                //#else
                //$$ disableSummoning()
                //$$ immuneToFire()
                //$$ size(0f, 0f)
                //$$ setUpdateInterval(Int.MAX_VALUE)
                //$$ setTrackingRange(256)
                //#endif
            //$$ }
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
            registerPortalAccessor { PortalEntityAccessor(NetherPortalEntity::class.java, it) }
        }
        if (enableEndPortals) {
            //#if MC>=11400
            //$$ registerEntityType(EndEntryPortalEntity.ID, ::EndEntryPortalEntity, EntityClassification.MISC) {
                //#if FABRIC>=1
                //$$ disableSummon()
                //$$ setImmuneToFire()
                //$$ size(EntityDimensions.fixed(0f, 0f))
                //$$ trackable(256, Int.MAX_VALUE)
                //#else
                //$$ disableSummoning()
                //$$ immuneToFire()
                //$$ size(0f, 0f)
                //$$ setUpdateInterval(Int.MAX_VALUE)
                //$$ setTrackingRange(256)
                //#endif
            //$$ }
            //$$ registerEntityType(EndExitPortalEntity.ID, ::EndExitPortalEntity, EntityClassification.MISC) {
                //#if FABRIC>=1
                //$$ disableSummon()
                //$$ setImmuneToFire()
                //$$ size(EntityDimensions.fixed(0f, 0f))
                //$$ trackable(256, Int.MAX_VALUE)
                //#else
                //$$ disableSummoning()
                //$$ immuneToFire()
                //$$ size(0f, 0f)
                //$$ setUpdateInterval(Int.MAX_VALUE)
                //$$ setTrackingRange(256)
                //#endif
            //$$ }
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
            registerPortalAccessor { PortalEntityAccessor(EndEntryPortalEntity::class.java, it) }
            registerPortalAccessor { PortalEntityAccessor(EndExitPortalEntity::class.java, it) }
        }
    }
}
