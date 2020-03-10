//#if MC<11400
package de.johni0702.minecraft.betterportals.impl.mekanism.common

import mekanism.api.Coord4D
import org.apache.logging.log4j.LogManager

internal val LOGGER = LogManager.getLogger("betterportals/mekanism")
const val MEKANISM_MOD_ID = "mekanism"
const val TELEPORTER_ID = "$MEKANISM_MOD_ID:mekanism_teleporter"

val coord4DComparator =
        Comparator.comparingInt<Coord4D> { it.dimensionId }.thenBy { it.x }.thenBy { it.y }.thenBy { it.z }
operator fun Coord4D.compareTo(other: Coord4D): Int = coord4DComparator.compare(this, other)
//#endif
