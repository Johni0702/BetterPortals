package de.johni0702.minecraft.view.common

import de.johni0702.minecraft.view.client.ClientViewAPI
import de.johni0702.minecraft.view.client.ClientWorldsManager
import de.johni0702.minecraft.view.client.render.RenderPass
import de.johni0702.minecraft.view.client.render.RenderPassManager
import de.johni0702.minecraft.view.server.ServerViewAPI
import de.johni0702.minecraft.view.server.ServerWorldsManager
import net.minecraft.client.Minecraft
import net.minecraft.network.NetHandlerPlayServer
import net.minecraftforge.fml.client.FMLClientHandler
import net.minecraftforge.fml.common.FMLCommonHandler
import net.minecraftforge.fml.common.Loader
import net.minecraftforge.fml.relauncher.Side

/**
 * Entry point into the view API. See [ClientViewAPI] and [ServerViewAPI]
 */
interface ViewAPI {
    companion object {
        @JvmStatic
        val instance by lazy {
            val mod = Loader.instance().indexedModList["betterportals"]!!.mod
            if (mod == null && FMLCommonHandler.instance().side == Side.CLIENT && FMLClientHandler.instance().hasError()) {
                // Forge has probably aborted mod loading and may wish to show a user-friendly error GUI,
                // so we need to return a dummy ViewAPI implementation as otherwise we would have to crash.
                FailureFallbackViewAPI
            } else {
                mod as ViewAPI
            }
        }
    }

    /**
     * Client-side entry point into the view API.
     * Not to be accessed from the server.
     */
    val client: ClientViewAPI

    /**
     * Server-side entry point into the view API.
     * Not to be accessed from the client.
     */
    val server: ServerViewAPI
}

private object FailureFallbackViewAPI : ViewAPI {
    override val client: ClientViewAPI by lazy { FailureFallbackClientViewAPI }
    override val server: ServerViewAPI by lazy { FailureFallbackServerViewAPI }

    private object FailureFallbackClientViewAPI : ClientViewAPI {
        override fun getWorldsManager(minecraft: Minecraft): ClientWorldsManager? = null
        override fun getRenderPassManager(minecraft: Minecraft): RenderPassManager = FailureFallbackRenderPassManager

        private object FailureFallbackRenderPassManager : RenderPassManager {
            override val root: RenderPass? = null
            override val current: RenderPass? = null
            override val previous: RenderPass? = null
        }
    }

    object FailureFallbackServerViewAPI : ServerViewAPI {
        override fun getWorldsManager(connection: NetHandlerPlayServer): ServerWorldsManager = throw UnsupportedOperationException()
    }
}
