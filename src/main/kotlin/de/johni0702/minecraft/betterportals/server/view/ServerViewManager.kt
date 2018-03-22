package de.johni0702.minecraft.betterportals.server.view

import de.johni0702.minecraft.betterportals.common.view.ViewManager
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.Vec3d
import net.minecraft.world.WorldServer
import net.minecraftforge.common.capabilities.Capability
import net.minecraftforge.common.capabilities.CapabilityInject
import net.minecraftforge.common.capabilities.ICapabilityProvider

/**
 * Manages views for a player.
 *
 * Obtain an instance for a `player` by calling `player.getCapability(ServerViewManager.CAP, null)` or `player.viewManager`.
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
     * @return The newly created view
     */
    fun createView(world: WorldServer, pos: Vec3d): ServerView

    /**
     * Flush all packets from all views.
     * View packets are normally queued and send in batches once per tick. This can be problematic when the client
     * expects a packet in a view to arrive before a packet on the main connection. In such cases, this method should
     * be called before sending the packet on the main connection.
     */
    fun flushPackets()

    companion object {
        val CAP get() = cap
        @CapabilityInject(ServerViewManager::class)
        private lateinit var cap: Capability<ServerViewManager>
    }

    class Provider(
            lazyManager: () -> ServerViewManager
    ) : ICapabilityProvider {
        private val manager by lazy { lazyManager() }

        override fun hasCapability(capability: Capability<*>, facing: EnumFacing?): Boolean =
                capability == ServerViewManager.CAP

        override fun <T : Any> getCapability(capability: Capability<T>, facing: EnumFacing?): T? = when(capability) {
            CAP -> CAP.cast(manager)
            else -> null
        }
    }
}

val EntityPlayerMP.viewManager get() = getCapability(ServerViewManager.CAP, null)!!