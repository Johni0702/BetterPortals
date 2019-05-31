package de.johni0702.minecraft.view.client

import de.johni0702.minecraft.view.common.ViewAPI
import de.johni0702.minecraft.view.common.ViewManager
import net.minecraft.client.Minecraft
import net.minecraft.client.entity.EntityPlayerSP

/**
 * Manages views sent from the server.
 *
 * Can be obtained from [ViewAPI.getViewManager].
 */
interface ClientViewManager : ViewManager {
    override val player: EntityPlayerSP
    override val views: List<ClientView>
    override val mainView: ClientView

    /**
     * The currently active view.
     * @see withView
     */
    val activeView: ClientView

    /**
     * Switches the currently active view to [view] for the duration of the [block].
     */
    fun <T> withView(view: ClientView, block: () -> T): T
}

/**
 * The client-side view manager responsible for this instance of the Minecraft client.
 */
val Minecraft.viewManager get() = ClientViewAPI.instance.getViewManager(this)
