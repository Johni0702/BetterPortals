package de.johni0702.minecraft.betterportals.client.view

import de.johni0702.minecraft.betterportals.common.view.ViewManager
import net.minecraft.client.entity.EntityPlayerSP

/**
 * Manages views sent from the server.
 *
 * Can be obtained from [de.johni0702.minecraft.betterportals.BetterPortalsMod.viewManager]
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