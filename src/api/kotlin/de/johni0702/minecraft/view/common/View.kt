package de.johni0702.minecraft.view.common

import de.johni0702.minecraft.view.client.ClientView
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.world.World

/**
 * Represents a player's view of a world.
 * Multiple views may exist of the same or multiple worlds but only one of which is the main view at any one time.
 * The main view is the one which has the actual player entity, is responsible for user input and by default is the only
 * view rendered.
 *
 * Note that the world being viewed only changes when the player re-spawns after death with this view active
 * or some other mod changes the dimension of the player with this view active.
 */
interface View {
    /**
     * The [ViewManager] instance which this view belongs to.
     */
    val manager: ViewManager

    /**
     * The unique id of this view.
     * This is the id used when communicating between client and server and otherwise holds no significance.
     */
    val id: Int

    /**
     * Whether this view is still valid.
     */
    val isValid: Boolean
        get() = true // TODO remove default impl in 0.3

    /**
     * Check if this view [isValid] and throw an exception if it is not.
     */
    fun checkValid() {
        check(isValid) { "View $this has already been destroyed" }
    }

    /**
     * Whether this view is the main view.
     * The camera in the main view is the actual player entity which other players will see.
     * User input is handled by the main view and by default the main view is the only view which is rendered.
     */
    val isMainView: Boolean
        get() = manager.mainView == this

    /**
     * The camera/player entity for this view.
     * For the [main view][isMainView], this is the ordinary player entity. For all other views, this is a special
     * entity which is invisible to all players and does not interact with the world.
     *
     * The camera entity may change when the main view changes or, if this is the main view, on player respawn.
     * However the world it resides in will only ever change when this is the main view and the player respawns. In
     * particular it will not change when the main view changes.
     *
     * Be aware of the difference between [player] and [ClientView.clientPlayer].
     */
    val player: EntityPlayer

    /**
     * The world of this view.
     */
    val world: World
}