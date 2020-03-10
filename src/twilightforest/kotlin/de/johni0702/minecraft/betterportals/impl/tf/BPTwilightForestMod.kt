//#if MC<11400
package de.johni0702.minecraft.betterportals.impl.tf

import de.johni0702.minecraft.betterportals.client.render.RenderOneWayPortalEntity
import de.johni0702.minecraft.betterportals.common.entity.PortalEntityAccessor
import de.johni0702.minecraft.betterportals.impl.BPConfig
import de.johni0702.minecraft.betterportals.impl.BlockRegistry
import de.johni0702.minecraft.betterportals.impl.EntityTypeRegistry
import de.johni0702.minecraft.betterportals.impl.ModBase
import de.johni0702.minecraft.betterportals.impl.registerPortalAccessor
import de.johni0702.minecraft.betterportals.impl.tf.client.renderer.TFPortalRenderer
import de.johni0702.minecraft.betterportals.impl.tf.common.TF_MOD_ID
import de.johni0702.minecraft.betterportals.impl.tf.common.blocks.BlockBetterTFPortal
import de.johni0702.minecraft.betterportals.impl.tf.common.entity.TFPortalEntity
import de.johni0702.minecraft.betterportals.impl.toConfiguration
import net.minecraft.util.ResourceLocation
import net.minecraftforge.fml.client.registry.RenderingRegistry
import net.minecraftforge.fml.common.Loader
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.registry.EntityRegistry

@Mod(modid = BPTwilightForestMod.MOD_ID, useMetadata = true)
class BPTwilightForestMod : ModBase() {
    companion object {
        const val MOD_ID = "betterportals-twilightforest"
        internal val PORTAL_CONFIG = BPConfig.twilightForestPortals.toConfiguration()
    }

    override val canLoad: Boolean by lazy { Loader.isModLoaded(TF_MOD_ID) }

    override fun BlockRegistry.registerBlocks() {
        register(BlockBetterTFPortal(this@BPTwilightForestMod))
    }

    override fun EntityTypeRegistry.registerEntities() {
        EntityRegistry.registerModEntity(
                ResourceLocation("betterportals", "tf_portal"),
                TFPortalEntity::class.java,
                "tf_portal",
                3,
                this@BPTwilightForestMod,
                256,
                Int.MAX_VALUE,
                false
        )
        registerPortalAccessor { PortalEntityAccessor(TFPortalEntity::class.java, it) }
    }

    override fun clientPreInit() {
        RenderingRegistry.registerEntityRenderingHandler(TFPortalEntity::class.java) {
            RenderOneWayPortalEntity(it, TFPortalRenderer(PORTAL_CONFIG.opacity))
        }
    }
}
//#endif