package de.johni0702.minecraft.betterportals.impl.server

import de.johni0702.minecraft.betterportals.common.pos
import de.johni0702.minecraft.betterportals.impl.common.forgeCapabilities
import de.johni0702.minecraft.betterportals.impl.net.TransferToDimension
import de.johni0702.minecraft.betterportals.impl.net.sendTo
import de.johni0702.minecraft.view.server.ServerView
import de.johni0702.minecraft.view.server.Ticket
import de.johni0702.minecraft.view.server.viewManager
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.server.management.PlayerList
import net.minecraftforge.common.util.ITeleporter

internal object DimensionTransitionHandler {
    val tickets = mutableMapOf<ServerView, Ticket>()

    fun transferPlayerToDimension(playerList: PlayerList, player: EntityPlayerMP, dimension: Int, teleporter: ITeleporter) {
        val world = player.server!!.getWorld(dimension)
        val viewManager = player.viewManager
        val oldView = viewManager.mainView

        // Hold on to the old main view until the client has finished the transition
        // (released in TransferToDimensionDone#Handler)
        tickets[oldView] = oldView.allocateExclusiveTicket() ?:
                // Even though optimally we'd want an exclusive ticket here, we're much more likely to get a fixed location one
                // and if they aren't moving super fast (and why would they during the transition?), that should do as well.
                oldView.allocateFixedLocationTicket() ?:
                // For maximum compatibility (and because we really only need it for 10 seconds), we'll even make due with a plain one.
                oldView.allocatePlainTicket()

        // Create a new view entity in the destination dimension
        val view = viewManager.createView(world, player.pos) {
            // Some teleporter require capabilities attached to the player but not the view entity (e.g. Cavern II)
            val oldCapabilities = forgeCapabilities
            forgeCapabilities = player.forgeCapabilities

            // Let the teleporter position the view entity
            playerList.transferEntityToWorld(this, player.dimension, world, world, teleporter)

            // Reset view entity capabilities since we're going to swap in the player soon enough anyway
            forgeCapabilities = oldCapabilities
        }

        // Start transaction to allow the handler of TransferToDimension to update the camera in the target dimension before switching to it
        viewManager.beginTransaction()

        // Inform the client of the transition to allow it to prepare any graphical transitions
        TransferToDimension(viewManager.mainView.id, view.id).sendTo(player)

        // And immediately swap it with the main view (calling code expects the player to have been transferred when the method returns)
        // This will inform the client that the server main view has changed and it'll adapt accordingly
        view.releaseAndMakeMainView(view.allocateExclusiveTicket()!!)

        viewManager.endTransaction()

        // Finally send a poslook packet to have the client confirm the teleport (and to be able to discard any UsePortal messages until the confirmation)
        player.connection.setPlayerLocation(player.posX, player.posY, player.posZ, player.rotationYaw, player.rotationPitch)
        viewManager.flushPackets()
    }
}