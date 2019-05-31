package de.johni0702.minecraft.view.client

import de.johni0702.minecraft.view.common.View
import net.minecraft.client.entity.EntityPlayerSP

interface ClientView : View {
    override val manager: ClientViewManager
    override val camera: EntityPlayerSP

    fun <T> withView(block: () -> T): T = manager.withView(this, block)
}