//#if MC<11400
package de.johni0702.minecraft.betterportals.impl.aether

import com.legacy.aether.blocks.BlocksAether
import de.johni0702.minecraft.betterportals.client.render.FramedPortalRenderer
import de.johni0702.minecraft.betterportals.client.render.RenderPortalEntity
import de.johni0702.minecraft.betterportals.common.entity.PortalEntityAccessor
import de.johni0702.minecraft.betterportals.impl.BPConfig
import de.johni0702.minecraft.betterportals.impl.BlockRegistry
import de.johni0702.minecraft.betterportals.impl.EntityTypeRegistry
import de.johni0702.minecraft.betterportals.impl.ModBase
import de.johni0702.minecraft.betterportals.impl.aether.common.AETHER_MOD_ID
import de.johni0702.minecraft.betterportals.impl.aether.common.blocks.BlockBetterAetherPortal
import de.johni0702.minecraft.betterportals.impl.aether.common.entity.AetherPortalEntity
import de.johni0702.minecraft.betterportals.impl.registerPortalAccessor
import de.johni0702.minecraft.betterportals.impl.toConfiguration
import net.minecraft.client.Minecraft
import net.minecraft.util.ResourceLocation
import net.minecraftforge.fml.client.registry.RenderingRegistry
import net.minecraftforge.fml.common.Loader
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.registry.EntityRegistry

@Mod(modid = BPAetherMod.MOD_ID, useMetadata = true)
class BPAetherMod : ModBase() {
    companion object {
        const val MOD_ID = "betterportals-aether"
        internal val PORTAL_CONFIG = BPConfig.aetherPortals.toConfiguration()
    }

    override val canLoad: Boolean by lazy { Loader.isModLoaded(AETHER_MOD_ID) }

    override fun BlockRegistry.registerBlocks() {
        register(BlockBetterAetherPortal(this@BPAetherMod).also { BlocksAether.aether_portal = it })
    }

    override fun EntityTypeRegistry.registerEntities() {
        EntityRegistry.registerModEntity(
                ResourceLocation("betterportals", "aether_portal"),
                AetherPortalEntity::class.java,
                "aether_portal",
                4,
                this@BPAetherMod,
                256,
                Int.MAX_VALUE,
                false
        )
        registerPortalAccessor { PortalEntityAccessor(AetherPortalEntity::class.java, it) }
    }

    override fun clientPreInit() {
        RenderingRegistry.registerEntityRenderingHandler(AetherPortalEntity::class.java) {
            RenderPortalEntity(it, FramedPortalRenderer(PORTAL_CONFIG.opacity, {
                Minecraft.getMinecraft().textureMapBlocks.getAtlasSprite("$AETHER_MOD_ID:blocks/aether_portal")
            }))
        }
    }
}
//#endif