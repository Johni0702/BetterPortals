package de.johni0702.minecraft.view.impl

import de.johni0702.minecraft.view.client.ClientViewAPI
import de.johni0702.minecraft.view.client.ClientViewManager
import de.johni0702.minecraft.view.client.render.RenderPassManager
import de.johni0702.minecraft.view.common.ViewAPI
import de.johni0702.minecraft.view.impl.client.ClientViewManagerImpl
import de.johni0702.minecraft.view.impl.client.ViewDemuxingTaskQueue
import de.johni0702.minecraft.view.impl.client.render.ViewRenderManager
import de.johni0702.minecraft.view.impl.compat.registerOptifineCompat
import de.johni0702.minecraft.view.server.ServerView
import de.johni0702.minecraft.view.server.ServerViewAPI
import de.johni0702.minecraft.view.server.ServerViewManager
import de.johni0702.minecraft.view.server.viewManager
import net.minecraft.client.Minecraft
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.network.NetHandlerPlayServer
import org.apache.logging.log4j.LogManager

internal val LOGGER = LogManager.getLogger("betterportals/view")
internal const val MOD_ID = "BP/view"

object ViewAPIImpl : ViewAPI {
    override val client: ClientViewAPI
        get() = ClientViewAPIImpl
    override val server: ServerViewAPI
        get() = ServerViewAPIImpl
}

internal object ClientViewAPIImpl : ClientViewAPI {
    internal val viewManagerImpl by lazy { ClientViewManagerImpl() }

    override fun getViewManager(minecraft: Minecraft): ClientViewManager? = if (minecraft.player == null) null else viewManagerImpl
    override fun getRenderPassManager(minecraft: Minecraft): RenderPassManager = ViewRenderManager.INSTANCE

    fun init() {
        viewManagerImpl.init()

        val mc = Minecraft.getMinecraft()
        synchronized(mc.scheduledTasks) {
            mc.scheduledTasks = ViewDemuxingTaskQueue(mc, mc.scheduledTasks)
        }

        registerOptifineCompat()
    }
}

internal object ServerViewAPIImpl : ServerViewAPI {
    override fun getViewManager(connection: NetHandlerPlayServer): ServerViewManager = (connection as IViewManagerHolder).viewManager
    override fun getView(player: EntityPlayerMP): ServerView? = player.viewManager.views.find { it.camera == player }
}

internal interface IViewManagerHolder {
    val viewManager: ServerViewManager
}
