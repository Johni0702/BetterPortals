package de.johni0702.minecraft.view.impl

import de.johni0702.minecraft.view.client.ClientViewAPI
import de.johni0702.minecraft.view.client.ClientWorldsManager
import de.johni0702.minecraft.view.client.render.RenderPassManager
import de.johni0702.minecraft.view.common.ViewAPI
import de.johni0702.minecraft.view.impl.client.ClientWorldsManagerImpl
import de.johni0702.minecraft.view.impl.client.ViewDemuxingTaskQueue
import de.johni0702.minecraft.view.impl.client.render.ViewRenderManager
import de.johni0702.minecraft.view.impl.compat.registerOptifineCompat
import de.johni0702.minecraft.view.impl.compat.registerVivecraftCompat
import de.johni0702.minecraft.view.impl.server.ServerWorldsManagerImpl
import de.johni0702.minecraft.view.server.ServerViewAPI
import de.johni0702.minecraft.view.server.ServerWorldsManager
import net.minecraft.client.Minecraft
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.network.NetHandlerPlayServer
import net.minecraft.util.math.Vec3d
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
    internal val viewManagerImpl by lazy { ClientWorldsManagerImpl() }

    override fun getWorldsManager(minecraft: Minecraft): ClientWorldsManager? = if (minecraft.player == null) null else viewManagerImpl
    override fun getRenderPassManager(minecraft: Minecraft): RenderPassManager = ViewRenderManager.INSTANCE

    fun init() {
        viewManagerImpl.init()

        val mc = Minecraft.getMinecraft()
        synchronized(mc.scheduledTasks) {
            mc.scheduledTasks = ViewDemuxingTaskQueue(mc, mc.scheduledTasks)
        }

        registerOptifineCompat()
        registerVivecraftCompat()
    }
}

internal object ServerViewAPIImpl : ServerViewAPI {
    override fun getWorldsManager(connection: NetHandlerPlayServer): ServerWorldsManager = connection.worldsManagerImpl
}

internal val NetHandlerPlayServer.worldsManagerImpl get() = (this as IWorldsManagerHolder).worldsManager
internal val EntityPlayerMP.worldsManagerImpl get() = connection.worldsManagerImpl
internal interface IWorldsManagerHolder {
    val worldsManager: ServerWorldsManagerImpl
}

internal interface IChunkCompileTaskGenerator {
    val viewerEyePos: Vec3d
}
@Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
class DummyEntity : Entity(null) {
    override fun writeEntityToNBT(compound: NBTTagCompound) = Unit
    override fun readEntityFromNBT(compound: NBTTagCompound) = Unit
    override fun entityInit() = Unit
}
