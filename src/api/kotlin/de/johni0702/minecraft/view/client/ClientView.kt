package de.johni0702.minecraft.view.client

import de.johni0702.minecraft.view.common.View
import de.johni0702.minecraft.view.server.CanMakeMainView
import de.johni0702.minecraft.view.server.ServerView
import net.minecraft.client.entity.EntityPlayerSP
import net.minecraft.client.multiplayer.WorldClient
import net.minecraft.network.NetHandlerPlayServer

interface ClientView : View {
    override val manager: ClientViewManager
    override val player: EntityPlayerSP

    /**
     * This is almost always the same as [player] except while views are being swapped.
     *
     * [player] refers to the logical player entity of the connection backing this view (i.e. the player from the
     * server's perspective):
     * - incoming packets are destined for it
     * - it only changes once the server acknowledges the swap (i.e. once it starts sending packets destined for this
     *   view on the other channel), not immediately when [makeMainView] is called
     * - it is always spawned in the world during packet handling
     *
     * [clientPlayer] refers to the user-controlled player entity (i.e. the player from the client's perspective):
     * - always matches [player] except while server and client main view differ
     * - outgoing packets are dispatched based on its actions
     * - changes immediately on call to [makeMainView]
     * - removed from this view's world during packet handling because packets aren't (yet) meant for it
     * - spawned in this view's world during tick and render (to give the user the impression of instantaneous world
     *   change, under the assumption that the server will ack the change)
     *   (if there's already an entity with the same id present, it is temporarily removed)
     * - for the [client main view][ClientViewManager.mainView] it matches the [player] of the
     *   [server main view][ClientViewManager.serverMainView]
     * - as such, it is spawned in the server main view's world during packet handling
     * - when teleported by the server, any un-acked view change is reversed client-side (the server will ignore
     *   packets until that teleport is acknowledged by the client anyway)
     */
    val clientPlayer: EntityPlayerSP

    /**
     * The world of this view.
     * Depending on context, this will either be the world of the [player] or the [clientPlayer].
     */
    override val world: WorldClient

    /**
     * Whether this view is the [server main view][ClientViewManager.serverMainView].
     */
    val isServerMainView: Boolean
        get() = manager.serverMainView == this

    /**
     * Changes this view to be the main view by swapping [player]/[clientPlayer] entities and designation with the
     * [current main view] [ClientViewManager.mainView]. Be aware of the difference between [player] and [clientPlayer].
     *
     * The position, rotation and some other state of the camera entities are swapped as well, such that `camera.pos`
     * is the same before and after this call. Or more generally, rendering the world with the camera before and after
     * the call will look (almost) the same.
     * Does nothing if this view [is the main view][isMainView].
     *
     * Note that this method **must not** be called while any significant view-dependent operation is in progress (e.g.
     * rendering, world ticking, packet handling).
     *
     * You **must** guarantee that you will call [ServerView.makeMainView] immediately following any packets sent
     * to the server by this method. I.e. you must send one of your own packets to the server immediately following this
     * call and then call [ServerView.makeMainView] in its handler.
     * As such, you may only call this method on views which you will have an [appropriate ticket][CanMakeMainView]
     * for on the server-side before the time your packet arrives (at the latest, by the time any packets sent from
     * this method arrive).
     *
     * The server may decide in the meantime that the player has performed invalid movement or needs to be teleported
     * for other reasons. In such cases, [ServerView.makeMainView] **must not** be called on the server and the client
     * view swap will automatically be rolled back upon reception of the teleport packet.
     * To detect this case in your server-side packet handler, check [NetHandlerPlayServer.targetPos] (as should be done
     * in most packet handlers anyway).
     */
    fun makeMainView()
}