package de.johni0702.minecraft.view.client

import de.johni0702.minecraft.view.common.View
import de.johni0702.minecraft.view.server.CanMakeMainView
import de.johni0702.minecraft.view.server.ServerView
import net.minecraft.client.entity.EntityPlayerSP

interface ClientView : View {
    override val manager: ClientViewManager
    override val camera: EntityPlayerSP

    fun <T> withView(block: () -> T): T = manager.withView(this, block)

    /**
     * Changes this view to be the main view by swapping [camera] entities and designation with the [current main view]
     * [ClientViewManager.mainView].
     *
     * The position, rotation and some other state of the camera entities are swapped as well, such that `camera.pos`
     * is the same before and after this call. Or more generally, rendering the world with the camera before and after
     * the call will look (almost) the same.
     * Does nothing if this view [is the main view][isMainView].
     *
     * Note that this method **must not** be called while any significant view-dependent operation is in progress (e.g.
     * rendering, world ticking, [ClientView.withView]).
     *
     * You **must** guarantee that you will call [ServerView.makeMainView] immediately following any packets sent
     * to the server by this method. I.e. you must send one of your own packets to the server immediately following this
     * call and then call [ServerView.makeMainView] in its handler.
     * As such, you may only call this method on views which you will have an [appropriate ticket][CanMakeMainView]
     * for on the server-side before the time your packet arrives (at the latest, by the time any packets sent from
     * this method arrive).
     *
     * TODO the server can reset the player (e.g. because of "invalid" movement), should talk about how that interacts
     */
    fun makeMainView()
}