package de.johni0702.minecraft.view.server

import de.johni0702.minecraft.view.common.ViewManager
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
     * To use the view, you **must** acquire a [Ticket] for it via any of its `allocate*Ticket` methods.
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

/**
 * The server-side view manager responsible for this connection.
 */
val NetHandlerPlayServer.viewManager get() = ServerViewAPI.instance.getViewManager(this)

/**
 * The server-side view manager responsible for this player.
 */
val EntityPlayerMP.viewManager get() = ServerViewAPI.instance.getViewManager(this)

/**
 * The server-side view for which this player is the camera.
 * Can return null e.g. for [net.minecraftforge.common.util.FakePlayer].
 */
val EntityPlayerMP.view get() = ServerViewAPI.instance.getView(this)