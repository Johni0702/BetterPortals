package de.johni0702.minecraft.betterportals.impl.nether

import de.johni0702.minecraft.betterportals.client.render.FramedPortalRenderer
import de.johni0702.minecraft.betterportals.client.render.RenderPortalEntity
import de.johni0702.minecraft.betterportals.common.PortalConfiguration
import de.johni0702.minecraft.betterportals.common.entity.PortalEntityAccessor
import de.johni0702.minecraft.betterportals.impl.BPConfig
import de.johni0702.minecraft.betterportals.impl.BlockRegistry
import de.johni0702.minecraft.betterportals.impl.registerEntityRenderer
import de.johni0702.minecraft.betterportals.impl.registerPortalAccessor
import de.johni0702.minecraft.betterportals.impl.EntityTypeRegistry
import de.johni0702.minecraft.betterportals.impl.ModBase
import de.johni0702.minecraft.betterportals.impl.toConfiguration
import de.johni0702.minecraft.betterportals.impl.nether.common.BlockWithBPVersion
import de.johni0702.minecraft.betterportals.impl.nether.common.entity.NetherPortalEntity
import net.minecraft.client.Minecraft
import net.minecraft.init.Blocks
import net.minecraft.util.ResourceLocation

//#if FABRIC>=1
//$$ import net.fabricmc.fabric.api.event.client.ClientSpriteRegistryCallback
//$$ import net.minecraft.client.texture.SpriteAtlasTexture
//$$ import net.minecraft.entity.EntityDimensions
//#else
import net.minecraftforge.fml.common.Mod
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
import net.minecraftforge.fml.common.Loader
import net.minecraftforge.fml.common.registry.EntityRegistry
//#endif

//#if FABRIC<1
//#if MC>=11400
//$$ @Mod(BPNetherMod.MOD_ID)
//#else
@Mod(modid = BPNetherMod.MOD_ID, useMetadata = true)
//#endif
//#endif
class BPNetherMod : ModBase() {
    companion object {
        const val MOD_ID = "betterportals-nether"
        internal val PORTAL_CONFIG = BPConfig.netherPortals.toConfiguration()
    }

    override fun BlockRegistry.registerBlocks() {
        //#if MC<11400
        if (Loader.isModLoaded("mekanism")) {
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

    override fun EntityTypeRegistry.registerEntities() {
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
                ResourceLocation("betterportals", "nether_portal"),
                NetherPortalEntity::class.java,
                "nether_portal",
                0,
                this@BPNetherMod,
                256,
                Int.MAX_VALUE,
                false
        )
        //#endif
        registerPortalAccessor { PortalEntityAccessor(NetherPortalEntity::class.java, it) }
    }

    override fun clientPreInit() {
        registerEntityRenderer<NetherPortalEntity> {
            RenderPortalEntity(it, FramedPortalRenderer(PORTAL_CONFIG.opacity, {
                //#if MC>=11400
                //$$ Minecraft.getInstance().textureMap.getSprite(ResourceLocation("minecraft", "block/nether_portal"))
                //#else
                Minecraft.getMinecraft().textureMapBlocks.getAtlasSprite("minecraft:blocks/portal")
                //#endif
            }))
        }
    }

    override fun commonPostInit() {
        (Blocks.PORTAL as BlockWithBPVersion).enableBetterVersion(this)
    }
}