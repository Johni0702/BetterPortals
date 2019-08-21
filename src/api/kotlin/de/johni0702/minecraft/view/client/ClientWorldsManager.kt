package de.johni0702.minecraft.view.client

import de.johni0702.minecraft.view.common.WorldsManager
import de.johni0702.minecraft.view.server.ServerWorldsManager
import net.minecraft.client.Minecraft
import net.minecraft.client.entity.EntityPlayerSP
import net.minecraft.client.multiplayer.WorldClient
import net.minecraft.network.NetHandlerPlayServer

/**
 * Manages worlds sent from the server.
 *
 * Can be obtained from [ClientViewAPI.getWorldsManager] or [Minecraft.worldsManager].
 */
interface ClientWorldsManager : WorldsManager {
    override val player: EntityPlayerSP
    override val worlds: List<WorldClient>

    /**
     * Transfers the main [player] into the specified world.
     *
     * Note that this method **must not** be called while any significant world-dependent operation is in progress (e.g.
     * rendering, world ticking, packet handling).
     *
     * You **must** guarantee that you will call [ServerWorldsManager.changeDimension] immediately following any packets
     * sent to the server by this method. I.e. you must send one of your own packets to the server immediately following
     * this call and then call [ServerWorldsManager.changeDimension] in its handler.
     *
     * The server may decide in the meantime that the player has performed invalid movement or needs to be teleported
     * for other reasons. In such cases, [ServerWorldsManager.changeDimension] **must not** be called on the server and
     * the client-side transition will automatically be rolled back upon reception of the teleport packet.
     * To detect this case in your server-side packet handler, check [NetHandlerPlayServer.targetPos] (as should be done
     * in most packet handlers anyway).
     */
    fun changeDimension(newWorld: WorldClient, updatePosition: EntityPlayerSP.() -> Unit)
}

/**
 * The client-side world manager responsible for this instance of the Minecraft client.
 * May be `null` when no world manager is currently present (e.g. while still in the main menu or not yet fully
 * connected).
 */
val Minecraft.worldsManager get() = ClientViewAPI.instance.getWorldsManager(this)

/**
 * The client-side world manager responsible for this instance of the Minecraft client.
 */
@Suppress("unused") // we could AT the `mc` field but this should do as well
val EntityPlayerSP.worldsManager get() = ClientViewAPI.instance.getWorldsManager(Minecraft.getMinecraft())!!
