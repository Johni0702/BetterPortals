package de.johni0702.minecraft.view.impl.server

import de.johni0702.minecraft.view.impl.LOGGER
import de.johni0702.minecraft.view.server.ExclusiveTicket
import de.johni0702.minecraft.view.server.FixedLocationTicket
import de.johni0702.minecraft.view.server.Ticket

internal open class TicketImpl(
        override val view: ServerViewImpl
) : Ticket {
    internal val allocStackTrace = Throwable().fillInStackTrace()
    override var released = false

    override fun release() {
        ensureValid(view)
        released = true
        view.releaseTicket(this)
    }

    protected fun finalize() {
        if (!released) {
            LOGGER.warn("View ticket dropped without being released! Originally allocated at:", allocStackTrace)
            released = true
        }
    }
}

internal class FixedLocationTicketImpl(view: ServerViewImpl) : TicketImpl(view), FixedLocationTicket
internal class ExclusiveTicketImpl(view: ServerViewImpl) : TicketImpl(view), ExclusiveTicket
