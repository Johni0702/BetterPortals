package de.johni0702.minecraft.view.client

import de.johni0702.minecraft.view.common.ViewManager
import net.minecraft.client.Minecraft
import net.minecraft.client.entity.EntityPlayerSP

/**
 * Manages views sent from the server.
 *
 * Can be obtained from [ClientViewAPI.getViewManager] or [Minecraft.viewManager].
 */
interface ClientViewManager : ViewManager {
    override val player: EntityPlayerSP
    override val views: List<ClientView>
    override val mainView: ClientView

    /**
     * Differs from [mainView] only after a call to [ClientView.makeMainView] until the server acknowledges the switch.
     * For that duration, this still refers to the old mainView and is the view on which incoming packets will be
     * handled.
     */
    val serverMainView: ClientView

    /**
     * The currently active view.
     */
    val activeView: ClientView
}

/**
 * The client-side view manager responsible for this instance of the Minecraft client.
 * May be `null` when no view manager is currently present (e.g. while still in the main menu or not yet fully
 * connected).
 */
val Minecraft.viewManager get() = ClientViewAPI.instance.getViewManager(this)
