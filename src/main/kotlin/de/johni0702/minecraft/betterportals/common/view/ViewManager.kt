package de.johni0702.minecraft.betterportals.common.view

import net.minecraft.entity.player.EntityPlayer

/**
 * Manages [View]s for a player.
 */
interface ViewManager {
    /**
     * The player whose views are managed.
     * Note that the actual player entity may change (e.g. on respawn).
     */
    val player: EntityPlayer

    /**
     * All views which currently exist for the player.
     */
    val views: List<View>

    /**
     * The main view.
     */
    val mainView: View
}