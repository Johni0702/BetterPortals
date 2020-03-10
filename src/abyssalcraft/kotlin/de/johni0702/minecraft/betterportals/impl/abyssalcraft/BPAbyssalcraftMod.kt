//#if MC<11400
package de.johni0702.minecraft.betterportals.impl.abyssalcraft

import com.shinoow.abyssalcraft.api.block.ACBlocks
import de.johni0702.minecraft.betterportals.client.render.FramedPortalRenderer
import de.johni0702.minecraft.betterportals.client.render.RenderPortalEntity
import de.johni0702.minecraft.betterportals.common.entity.PortalEntityAccessor
import de.johni0702.minecraft.betterportals.impl.BPConfig
import de.johni0702.minecraft.betterportals.impl.BlockRegistry
import de.johni0702.minecraft.betterportals.impl.EntityTypeRegistry
import de.johni0702.minecraft.betterportals.impl.ModBase
import de.johni0702.minecraft.betterportals.impl.abyssalcraft.common.ABYSSALCRAFT_MOD_ID
import de.johni0702.minecraft.betterportals.impl.abyssalcraft.common.blocks.BlockBetterAbyssPortal
import de.johni0702.minecraft.betterportals.impl.abyssalcraft.common.blocks.BlockBetterDreadlandsPortal
import de.johni0702.minecraft.betterportals.impl.abyssalcraft.common.blocks.BlockBetterOmotholPortal
import de.johni0702.minecraft.betterportals.impl.abyssalcraft.common.entity.AbyssPortalEntity
import de.johni0702.minecraft.betterportals.impl.abyssalcraft.common.entity.DreadlandsPortalEntity
import de.johni0702.minecraft.betterportals.impl.abyssalcraft.common.entity.OmotholPortalEntity
import de.johni0702.minecraft.betterportals.impl.registerPortalAccessor
import de.johni0702.minecraft.betterportals.impl.toConfiguration
import net.minecraft.client.Minecraft
import net.minecraft.util.ResourceLocation
import net.minecraftforge.fml.client.registry.RenderingRegistry
import net.minecraftforge.fml.common.Loader
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.registry.EntityRegistry

@Mod(modid = BPAbyssalcraftMod.MOD_ID, useMetadata = true)
class BPAbyssalcraftMod : ModBase() {
    companion object {
        const val MOD_ID = "betterportals-abyssalcraft"
        internal val PORTAL_CONFIG = BPConfig.abyssalcraftPortals.toConfiguration()
    }

    override val canLoad: Boolean by lazy { Loader.isModLoaded(ABYSSALCRAFT_MOD_ID) }

    override fun BlockRegistry.registerBlocks() {
        register(BlockBetterAbyssPortal(this@BPAbyssalcraftMod).also { ACBlocks.abyssal_gateway = it })
        register(BlockBetterDreadlandsPortal(this@BPAbyssalcraftMod).also { ACBlocks.dreaded_gateway = it })
        register(BlockBetterOmotholPortal(this@BPAbyssalcraftMod).also { ACBlocks.omothol_gateway = it })
    }

    override fun EntityTypeRegistry.registerEntities() {
        EntityRegistry.registerModEntity(
                ResourceLocation("betterportals", "${ABYSSALCRAFT_MOD_ID}_abyssal_portal"),
                AbyssPortalEntity::class.java,
                "${ABYSSALCRAFT_MOD_ID}_abyssal_portal",
                5,
                this@BPAbyssalcraftMod,
                256,
                Int.MAX_VALUE,
                false
        )
        EntityRegistry.registerModEntity(
                ResourceLocation("betterportals", "${ABYSSALCRAFT_MOD_ID}_dreadlands_portal"),
                DreadlandsPortalEntity::class.java,
                "${ABYSSALCRAFT_MOD_ID}_dreadlands_portal",
                6,
                this@BPAbyssalcraftMod,
                256,
                Int.MAX_VALUE,
                false
        )
        EntityRegistry.registerModEntity(
                ResourceLocation("betterportals", "${ABYSSALCRAFT_MOD_ID}_omothol_portal"),
                OmotholPortalEntity::class.java,
                "${ABYSSALCRAFT_MOD_ID}_omothol_portal",
                7,
                this@BPAbyssalcraftMod,
                256,
                Int.MAX_VALUE,
                false
        )
        registerPortalAccessor { PortalEntityAccessor(AbyssPortalEntity::class.java, it) }
        registerPortalAccessor { PortalEntityAccessor(DreadlandsPortalEntity::class.java, it) }
        registerPortalAccessor { PortalEntityAccessor(OmotholPortalEntity::class.java, it) }
    }

    override fun clientPreInit() {
        RenderingRegistry.registerEntityRenderingHandler(AbyssPortalEntity::class.java) {
            RenderPortalEntity(it, FramedPortalRenderer(PORTAL_CONFIG.opacity, {
                Minecraft.getMinecraft().textureMapBlocks.getAtlasSprite("$ABYSSALCRAFT_MOD_ID:blocks/ag")
            }))
        }
        RenderingRegistry.registerEntityRenderingHandler(DreadlandsPortalEntity::class.java) {
            RenderPortalEntity(it, FramedPortalRenderer(PORTAL_CONFIG.opacity, {
                Minecraft.getMinecraft().textureMapBlocks.getAtlasSprite("$ABYSSALCRAFT_MOD_ID:blocks/dg")
            }))
        }
        RenderingRegistry.registerEntityRenderingHandler(OmotholPortalEntity::class.java) {
            RenderPortalEntity(it, FramedPortalRenderer(PORTAL_CONFIG.opacity, {
                Minecraft.getMinecraft().textureMapBlocks.getAtlasSprite("$ABYSSALCRAFT_MOD_ID:blocks/og")
            }))
        }
    }
}
//#endif