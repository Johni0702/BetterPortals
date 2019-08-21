package de.johni0702.minecraft.view.server

import de.johni0702.minecraft.view.client.ClientWorldsManager
import de.johni0702.minecraft.view.common.WorldsManager
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.network.NetHandlerPlayServer
import net.minecraft.network.Packet
import net.minecraft.util.math.Vec3d
import net.minecraft.util.math.Vec3i
import net.minecraft.world.WorldServer

/**
 * Manages syncing of worlds for a player.
 *
 * Obtain an instance for a `player` via `player.connection.worldsManager` or `player.worldsManager`.
 */
interface ServerWorldsManager : WorldsManager {
    override val player: EntityPlayerMP
    override val worlds: List<WorldServer>

    val views: Map<WorldServer, List<View>>

    /**
     * Creates and registers a new view of [world] at [pos] with default view distance.
     *
     * @param world World of which the view is created
     * @param pos The center position of the view
     * @param anchor See [View.anchor]
     * @return The newly created view
     */
    fun createView(world: WorldServer, pos: Vec3d, anchor: Pair<WorldServer, Vec3i>? = null): View

    /**
     * Registers a new view with custom implementation.
     */
    fun registerView(view: View)

    /**
     * Transfers the main [player] into the specified world.
     *
     * Note that this method **must not** be called while any significant world-dependent operation is in progress (e.g.
     * world ticking).
     *
     * See [ClientWorldsManager.changeDimension].
     */
    fun changeDimension(newWorld: WorldServer, updatePosition: EntityPlayerMP.() -> Unit)

    /**
     * Sends the given packet in the given world.
     */
    fun sendPacket(world: WorldServer, packet: Packet<*>)

    /**
     * Flush all packets in all worlds.
     * Packets are normally queued and send in batches once per tick. This can be problematic when the client
     * expects a packet in a world to arrive before a packet on the main connection. In such cases, this method should
     * be called before sending the packet on the main connection.
     */
    fun flushPackets()

    /**
     * For certain things to look correct on the client, they need to happen all at once (e.g. despawning an entity
     * in one world, telling the client where it will reappear and then spawning it in another world) , i.e. they must
     * not be executed just partially with network lag in between, nor should the client render any frames between
     * processing these packets.
     * To make this happen, such actions need to happen within a single transaction.
     *
     * Calling this method starts a new transaction. Transactions may be nested.
     *
     * Starting/ending a transaction will also automatically ensure all packets are [flushed][flushPackets].
     */
    fun beginTransaction()

    /**
     * Ends a transaction started with [beginTransaction].
     *
     * Ending a transaction when none is in progress is an error.
     */
    fun endTransaction()
}

/**
 * The server-side worlds manager responsible for this connection.
 */
val NetHandlerPlayServer.worldsManager get() = ServerViewAPI.instance.getWorldsManager(this)

/**
 * The server-side worlds manager responsible for this player.
 */
val EntityPlayerMP.worldsManager get() = ServerViewAPI.instance.getWorldsManager(this)
