package de.johni0702.minecraft.betterportals.impl.client

import de.johni0702.minecraft.view.common.fabricEvent
import net.minecraftforge.fml.common.eventhandler.Event

internal class PostSetupFogEvent : Event()
{ companion object { @Suppress("unused") @JvmField val EVENT = fabricEvent<PostSetupFogEvent>() } }
