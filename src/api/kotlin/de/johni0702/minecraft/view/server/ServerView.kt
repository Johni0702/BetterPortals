package de.johni0702.minecraft.view.server

import de.johni0702.minecraft.view.common.View
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.world.WorldServer

/**
 * New views can be created by calling [ServerViewManager.createView].
 *
 * To use a view, you **must** acquire a [Ticket] for it via any of the `allocate*Ticket` methods.
 */
interface ServerView : View {
    override val manager: ServerViewManager
    override val player: EntityPlayerMP
    override val world: WorldServer
        get() = player.serverWorld

    /**
     * Changes this view to be the main view by swapping [player] entities and designation with the [current main view]
     * [ServerViewManager.mainView].
     *
     * The position, rotation and some other state of the camera entities are swapped as well, such that `camera.pos`
     * is the same before and after this call. Or more generally, rendering the world with the camera before and after
     * the call will look (almost) the same.
     *
     * It is an error to call this method if the view already [is the main view][isMainView].
     *
     * Note that this method **must not** be called while any significant view-dependent operation is in progress (e.g.
     * world ticking).
     *
     * If you no longer require the ticket after the change, consider using [releaseAndMakeMainView] instead.
     */
    fun makeMainView(ticket: CanMakeMainView)

    /**
     * Same as [makeMainView] except the ticket is released before the main view is changed.
     * This has the advantage that third parties can acquire (otherwise conflicting) tickets as a direction reaction
     * to the change.
     */
    fun releaseAndMakeMainView(ticket: CanMakeMainView)

    /**
     * Allocates a plain [Ticket].
     */
    fun allocatePlainTicket(): Ticket

    /**
     * Allocates a fixed location ticket.
     *
     * Returns `null` if a such a ticket cannot currently be allocated. In most cases, you will want to create your own
     * view if that happens.
     */
    fun allocateFixedLocationTicket(): FixedLocationTicket?

    /**
     * Allocates an exclusive ticket.
     *
     * Returns `null` if a such a ticket cannot currently be allocated. In most cases, you will want to create your own
     * view if that happens.
     */
    fun allocateExclusiveTicket(): ExclusiveTicket?
}