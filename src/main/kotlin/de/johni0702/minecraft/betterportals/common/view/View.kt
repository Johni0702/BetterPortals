package de.johni0702.minecraft.betterportals.common.view

import de.johni0702.minecraft.betterportals.client.view.ClientView
import net.minecraft.entity.player.EntityPlayer

/**
 * Provides a view of a different world to a player.
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
     * Whether this view is the main view.
     * The camera in the main view is the actual player entity which other players will see.
     * Use input is handled by the main view and by default the main view is the only view which is rendered.
     */
    val isMainView: Boolean
        get() = manager.mainView == this

    /**
     * The camera entity for this view.
     * For the [main view][isMainView], this is the ordinary player entity. For all other views, this is a special
     * entity which is invisible to all players and does not interact with the world.
     * While the [camera] may change after [makeMainView], the world it's in will not.
     */
    val camera: EntityPlayer

    /**
     * Changes this view to be the main view by swapping [camera] entities and designation with the [current main view]
     * [ViewManager.mainView].
     * The position, rotation and some other state of the camera entities are swapped as well, such that `camera.pos`
     * is the same before and after this call. Or more generally, rendering the world with the camera before and after
     * the call will look (almost) the same.
     * Does nothing if this view [is the main view][isMainView].
     * Note that this method **must not** be called while any significant view-dependent operation is in progress (e.g.
     * rendering, world ticking, [ClientView.withView]).
     * If views are reference counted (server-side), calling this method decrements the reference count of the old main
     * view and increments the one of this view.
     */
    fun makeMainView()
}