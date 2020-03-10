package de.johni0702.minecraft.betterportals.impl.end

import de.johni0702.minecraft.betterportals.client.render.RenderOneWayPortalEntity
import de.johni0702.minecraft.betterportals.common.entity.PortalEntityAccessor
import de.johni0702.minecraft.betterportals.impl.BPConfig
import de.johni0702.minecraft.betterportals.impl.registerBlockEntityRenderer
import de.johni0702.minecraft.betterportals.impl.registerEntityRenderer
import de.johni0702.minecraft.betterportals.impl.registerPortalAccessor
import de.johni0702.minecraft.betterportals.impl.EntityTypeRegistry
import de.johni0702.minecraft.betterportals.impl.ModBase
import de.johni0702.minecraft.betterportals.impl.TileEntityTypeRegistry
import de.johni0702.minecraft.betterportals.impl.toConfiguration
import de.johni0702.minecraft.betterportals.impl.end.client.renderer.EndPortalRenderer
import de.johni0702.minecraft.betterportals.impl.end.client.tile.renderer.BetterEndPortalTileRenderer
import de.johni0702.minecraft.betterportals.impl.end.common.BlockWithBPVersion
import de.johni0702.minecraft.betterportals.impl.end.common.blocks.TileEntityBetterEndPortal
import de.johni0702.minecraft.betterportals.impl.end.common.entity.EndEntryPortalEntity
import de.johni0702.minecraft.betterportals.impl.end.common.entity.EndExitPortalEntity
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
//$$ import de.johni0702.minecraft.betterportals.impl.register
//$$ import de.johni0702.minecraft.betterportals.impl.registerEntityType
//$$ import net.minecraft.tileentity.TileEntityType
//$$ import net.minecraft.util.SharedConstants
//$$ import net.minecraft.util.datafix.DataFixesManager
//$$ import net.minecraft.util.datafix.TypeReferences
//$$ import java.util.function.Supplier
//#else
import net.minecraft.tileentity.TileEntity
import net.minecraftforge.fml.common.registry.EntityRegistry
//#endif

//#if FABRIC<1
//#if MC>=11400
//$$ @Mod(BPEndMod.MOD_ID)
//#else
@Mod(modid = BPEndMod.MOD_ID, useMetadata = true)
//#endif
//#endif
class BPEndMod : ModBase() {
    companion object {
        const val MOD_ID = "betterportals-end"
        internal val PORTAL_CONFIG = BPConfig.endPortals.toConfiguration()
    }

    override fun TileEntityTypeRegistry.registerBlockEntities() {
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

    override fun EntityTypeRegistry.registerEntities() {
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
                ResourceLocation("betterportals", "end_entry_portal"),
                EndEntryPortalEntity::class.java,
                "end_entry_portal",
                1,
                this@BPEndMod,
                256,
                Int.MAX_VALUE,
                false
        )
        EntityRegistry.registerModEntity(
                ResourceLocation("betterportals", "end_exit_portal"),
                EndExitPortalEntity::class.java,
                "end_exit_portal",
                2,
                this@BPEndMod,
                256,
                Int.MAX_VALUE,
                false
        )
        //#endif
        registerPortalAccessor { PortalEntityAccessor(EndEntryPortalEntity::class.java, it) }
        registerPortalAccessor { PortalEntityAccessor(EndExitPortalEntity::class.java, it) }
    }

    override fun clientPreInit() {
        registerBlockEntityRenderer<TileEntityBetterEndPortal>(BetterEndPortalTileRenderer())
        registerEntityRenderer<EndEntryPortalEntity> {
            RenderOneWayPortalEntity(it, EndPortalRenderer(PORTAL_CONFIG.opacity))
        }
        registerEntityRenderer<EndExitPortalEntity> {
            RenderOneWayPortalEntity(it, EndPortalRenderer(PORTAL_CONFIG.opacity))
        }
    }

    override fun commonPostInit() {
        (Blocks.END_PORTAL as BlockWithBPVersion).enableBetterVersion(this)
    }
}