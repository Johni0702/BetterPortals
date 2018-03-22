package de.johni0702.minecraft.betterportals.client.view

import de.johni0702.minecraft.betterportals.common.view.View
import net.minecraft.client.entity.EntityPlayerSP

interface ClientView : View {
    override val manager: ClientViewManager
    override val camera: EntityPlayerSP

    fun <T> withView(block: () -> T): T = manager.withView(this, block)
}