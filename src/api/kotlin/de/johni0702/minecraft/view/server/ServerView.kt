package de.johni0702.minecraft.view.server

import de.johni0702.minecraft.view.common.View
import io.netty.util.ReferenceCounted
import net.minecraft.entity.player.EntityPlayerMP

/**
 * New views can be created by calling [ServerViewManager.createView].
 * These views are reference counted. Call [retain] when using a view to prevent it from becoming destroyed,
 * call [release] if you no longer need the view and want to allow it to be destroyed.
 */
interface ServerView : View, ReferenceCounted {
    override val manager: ServerViewManager
    override val camera: EntityPlayerMP
}