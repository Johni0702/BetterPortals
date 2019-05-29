package de.johni0702.minecraft.betterportals.client

import de.johni0702.minecraft.betterportals.client.renderer.ViewRenderPlan
import net.minecraftforge.fml.common.eventhandler.Event

class PostSetupFogEvent : Event()

class PreRenderView(
        val plan: ViewRenderPlan,
        val partialTicks: Float
) : Event()
