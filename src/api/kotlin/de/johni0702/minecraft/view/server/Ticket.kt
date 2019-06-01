package de.johni0702.minecraft.view.server

/**
 * [ServerView]s can be shared between multiple users and as such need to be reference counted.
 * Each user of a view must acquire a [Ticket] if it wants to use it in any significant way.
 *
 * You must always manually release any [Ticket]s you no longer require. Do **not** let the garbage collector take care
 * of that, it's by its nature too unpredictable.
 * Views without any active tickets will regularly (but not immediately) be destroyed. It is always safe to release
 * your ticket and then immediately acquire a new (potentially different) one.
 *
 * Different ticket types exist to express different usage intentions.
 * Users which have not acquired a special kind of ticket must assume nothing about the view. In particular it may move
 * to anywhere else in the world at any time or even switch worlds entirely.
 * They also must not make any significant changes to the view as other users might have certain expectations for it.
 *
 * A special exception is the main view since it is inherently tied to the real player entity. Any ticket to a view
 * effectively degrades into a plain [Ticket] while it is the main view (rules as to who can make it the main view
 * of course still apply while it is not yet the main view).
 * The ticket must still be released if no longer required or can be held on to and will regain its guarantees and
 * privileges once another view has become the main view (though ofc at this point the location of the view will
 * have changed).
 */
interface Ticket {
    /**
     * The view corresponding to this ticket.
     */
    val view: ServerView

    /**
     * Whether this ticket has been released by a call to [release].
     */
    val released: Boolean

    /**
     * Releases this ticket.
     */
    fun release()

    /**
     * Ensures that this ticket has not yet been released and is valid for the given view.
     * Throws an exception otherwise.
     */
    fun ensureValid(view: ServerView) {
        if (view != this.view) {
            throw IllegalArgumentException("Ticket is for a different view.")
        }
        if (released) {
            throw IllegalStateException("Ticket has already been released.")
        }
    }
}

/**
 * Any [Ticket] which implements this interface allows any of its holders to make the corresponding view the main
 * view.
 * Beware of the implications detailed in [Ticket] which come with making a view the main view.
 */
interface CanMakeMainView : Ticket

/**
 * Holders of this [Ticket] are guaranteed that the current location of the view does not change (until one of them
 * decides to make it the main view).
 */
interface FixedLocationTicket : Ticket, CanMakeMainView

/**
 * Holders of this [Ticket] are guaranteed exclusive access to the view and are solely responsible for what happens to
 * it.
 * Once there exist any exclusive tickets for a view, only plain [Ticket]s can be acquired for it until the exclusive
 * one has been released.
 */
interface ExclusiveTicket : Ticket, CanMakeMainView