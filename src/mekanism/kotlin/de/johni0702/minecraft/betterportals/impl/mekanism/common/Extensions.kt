//#if MC<11400
package de.johni0702.minecraft.betterportals.impl.mekanism.common

import de.johni0702.minecraft.betterportals.common.PortalConfiguration
import de.johni0702.minecraft.betterportals.common.portalManager
import de.johni0702.minecraft.betterportals.common.tile.PortalTileEntityAccessor
import de.johni0702.minecraft.betterportals.impl.mekanism.client.tile.renderer.RenderBetterTeleporter
import de.johni0702.minecraft.betterportals.impl.mekanism.common.tile.TileEntityBetterTeleporter
import mekanism.api.Coord4D
import mekanism.common.block.states.BlockStateMachine
import net.minecraft.tileentity.TileEntity
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.client.registry.ClientRegistry
import net.minecraftforge.fml.common.Loader
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import org.apache.logging.log4j.LogManager
import java.util.function.Supplier

internal val LOGGER = LogManager.getLogger("betterportals/mekanism")
const val MEKANISM_MOD_ID = "mekanism"
const val TELEPORTER_ID = "$MEKANISM_MOD_ID:mekanism_teleporter"

lateinit var CONFIG_MEKANISM_PORTALS: PortalConfiguration

private val hasMekanism by lazy { Loader.isModLoaded(MEKANISM_MOD_ID) }

fun initMekanism(
        init: (() -> Unit) -> Unit,
        postInit: (() -> Unit) -> Unit,
        clientPostInit: (() -> Unit) -> Unit,
        enableMekanismPortals: Boolean,
        configMekanismPortals: PortalConfiguration
) {
    CONFIG_MEKANISM_PORTALS = configMekanismPortals

    if (!enableMekanismPortals || !hasMekanism) {
        return
    }

    postInit {
        TileEntity.register(TELEPORTER_ID, TileEntityBetterTeleporter::class.java)
        BlockStateMachine.MachineType.TELEPORTER.tileEntitySupplier = Supplier { TileEntityBetterTeleporter() }
    }

    clientPostInit {
        ClientRegistry.bindTileEntitySpecialRenderer(TileEntityBetterTeleporter::class.java, RenderBetterTeleporter(configMekanismPortals.opacity))
    }

    init {
        MinecraftForge.EVENT_BUS.register(object {
            @SubscribeEvent
            fun onWorld(event: WorldEvent.Load) {
                val world = event.world
                event.world.portalManager.registerPortals(PortalTileEntityAccessor(TileEntityBetterTeleporter::class.java, world))
            }
        })
    }
}

val coord4DComparator =
        Comparator.comparingInt<Coord4D> { it.dimensionId }.thenBy { it.x }.thenBy { it.y }.thenBy { it.z }
operator fun Coord4D.compareTo(other: Coord4D): Int = coord4DComparator.compare(this, other)
//#endif
