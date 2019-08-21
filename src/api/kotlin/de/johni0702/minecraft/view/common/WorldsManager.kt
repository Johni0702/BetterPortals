package de.johni0702.minecraft.view.common

import de.johni0702.minecraft.view.server.View
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.world.World

/**
 * Manages syncing of any number of [World]s for a player.
 */
interface WorldsManager {
    /**
     * The player whose worlds are managed.
     * Note that the actual player entity may change (e.g. on respawn).
     */
    val player: EntityPlayer

    /**
     * All worlds for which [View]s currently exist, i.e. all worlds synced with the client.
     */
    val worlds: List<World>
}