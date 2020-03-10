//#if MC<11400
package de.johni0702.minecraft.betterportals.impl.mekanism

import de.johni0702.minecraft.betterportals.common.tile.PortalTileEntityAccessor
import de.johni0702.minecraft.betterportals.impl.BPConfig
import de.johni0702.minecraft.betterportals.impl.ModBase
import de.johni0702.minecraft.betterportals.impl.mekanism.client.tile.renderer.RenderBetterTeleporter
import de.johni0702.minecraft.betterportals.impl.mekanism.common.MEKANISM_MOD_ID
import de.johni0702.minecraft.betterportals.impl.mekanism.common.TELEPORTER_ID
import de.johni0702.minecraft.betterportals.impl.mekanism.common.tile.TileEntityBetterTeleporter
import de.johni0702.minecraft.betterportals.impl.registerPortalAccessor
import de.johni0702.minecraft.betterportals.impl.toConfiguration
import mekanism.common.block.states.BlockStateMachine
import net.minecraft.tileentity.TileEntity
import net.minecraftforge.fml.client.registry.ClientRegistry
import net.minecraftforge.fml.common.Loader
import net.minecraftforge.fml.common.Mod
import java.util.function.Supplier

@Mod(modid = BPMekanismMod.MOD_ID, useMetadata = true)
class BPMekanismMod : ModBase() {
    companion object {
        const val MOD_ID = "betterportals-mekanism"
        internal val PORTAL_CONFIG = BPConfig.mekanismPortals.toConfiguration()
    }

    override val canLoad: Boolean by lazy { Loader.isModLoaded(MEKANISM_MOD_ID) }

    override fun commonInit() {
        registerPortalAccessor { PortalTileEntityAccessor(TileEntityBetterTeleporter::class.java, it) }
    }

    override fun commonPostInit() {
        TileEntity.register(TELEPORTER_ID, TileEntityBetterTeleporter::class.java)
        BlockStateMachine.MachineType.TELEPORTER.tileEntitySupplier = Supplier { TileEntityBetterTeleporter() }
    }

    override fun clientPostInit() {
        ClientRegistry.bindTileEntitySpecialRenderer(TileEntityBetterTeleporter::class.java, RenderBetterTeleporter(PORTAL_CONFIG.opacity))
    }
}
//#endif