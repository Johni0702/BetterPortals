package de.johni0702.minecraft.betterportals.common.util

import net.minecraft.profiler.Profiler
import net.minecraft.world.World
import java.util.*

class TickTimer(private val profiler: Profiler, private val period: Int, private var delay: Int = 0) {
    constructor(profiler: Profiler, period: Int, random: Random) : this(profiler, period, random.nextInt(period))
    constructor(period: Int, world: World) : this(world.profiler, period, world.rand)

    fun tick(profilerSection: String, elapsed: () -> Unit) {
        if (delay <= 0) {
            delay = period
            profiler.startSection(profilerSection)
            elapsed()
            profiler.endSection()
        } else {
            delay--
        }
    }
}
