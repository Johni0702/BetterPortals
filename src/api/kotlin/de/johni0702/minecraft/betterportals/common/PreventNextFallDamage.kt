package de.johni0702.minecraft.betterportals.common

import net.minecraft.entity.player.EntityPlayerMP
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.entity.living.LivingFallEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent

/**
 * Suppresses the next fall damage a player will take (within 10 seconds).
 */
class PreventNextFallDamage(
        private val player: EntityPlayerMP,

        /**
         * After this timeout (in ticks) reaches zero, we stop listening and assume the player somehow managed to not
         * take fall damage.
         */
        private var timeoutTicks: Int = 10 * 20 // 10 seconds
) {
    private var registered by MinecraftForge.EVENT_BUS

    init {
        registered = true
    }

    @SubscribeEvent
    fun onLivingFall(event: LivingFallEvent) {
        if (event.entity !== player) return // Note: cannot use != because Entity overwrites .equals
        event.isCanceled = true
        registered = false
    }

    @SubscribeEvent
    fun onTick(event: TickEvent.ServerTickEvent) {
        if (event.phase != TickEvent.Phase.START) return

        timeoutTicks--
        if (timeoutTicks <= 0) {
            registered = false
        }
    }
}

