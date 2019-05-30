package de.johni0702.minecraft.betterportals.server.view

import de.johni0702.minecraft.betterportals.common.view.ViewManager
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.network.NetHandlerPlayServer
import net.minecraft.util.math.Vec3d
import net.minecraft.world.WorldServer

/**
 * Manages views for a player.
 *
 * Obtain an instance for a `player` via `player.connection.viewManager` or `player.viewManager`.
 */
interface ServerViewManager : ViewManager {
    override val player: EntityPlayerMP
    override val views: List<ServerView>
    override val mainView: ServerView

    /**
     * Create a new view of [world] at [pos].
     *
     * The view is created with a reference count of one. Unless [ServerView.release] is called, the view will never be
     * destroyed (until the player disconnects).
     *
     * @param world World of which the view is created
     * @param pos The position where the newly created camera will be placed
     * @param beforeSendChunks Called on the view camera before any chunks are sent. Can be used to re-position the camera.
     * @return The newly created view
     */
    fun createView(world: WorldServer, pos: Vec3d, beforeSendChunks: EntityPlayerMP.() -> Unit = {}): ServerView

    /**
     * Flush all packets from all views.
     * View packets are normally queued and send in batches once per tick. This can be problematic when the client
     * expects a packet in a view to arrive before a packet on the main connection. In such cases, this method should
     * be called before sending the packet on the main connection.
     */
    fun flushPackets()
}

internal interface IViewManagerHolder {
    val viewManager: ServerViewManager
}
val NetHandlerPlayServer.viewManager get() = (this as IViewManagerHolder).viewManager
val EntityPlayerMP.viewManager get() = connection.viewManager