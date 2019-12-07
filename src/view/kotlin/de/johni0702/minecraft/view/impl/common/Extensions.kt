package de.johni0702.minecraft.view.impl.common

import de.johni0702.minecraft.betterportals.impl.accessors.AccLazyLoadBase
import de.johni0702.minecraft.view.impl.ClientViewAPIImpl
import de.johni0702.minecraft.view.impl.client.ViewDemuxingTaskQueue
import de.johni0702.minecraft.view.impl.client.render.ViewRenderManager
import de.johni0702.minecraft.view.impl.net.Net
import net.minecraft.client.Minecraft
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.LazyLoadBase

//#if MC>=11400
//#else
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.ListenableFutureTask
import de.johni0702.minecraft.betterportals.impl.accessors.AccMinecraft
import de.johni0702.minecraft.view.impl.LOGGER
import java.util.concurrent.Executors
//#endif

fun initView(
        init: (() -> Unit) -> Unit,
        clientInit: (() -> Unit) -> Unit,
        debugView: () -> Boolean
) {
    init {
        Net.INSTANCE // initialize via <init>
    }

    clientInit {
        ViewRenderManager.INSTANCE.debugView = debugView
        ClientViewAPIImpl.init()
    }
}

internal fun EntityPlayer.swapPosRotWith(e2: EntityPlayer) {
    val e1 = this

    e1.posX = e2.posX.also { e2.posX = e1.posX }
    e1.posY = e2.posY.also { e2.posY = e1.posY }
    e1.posZ = e2.posZ.also { e2.posZ = e1.posZ }
    e1.prevPosX = e2.prevPosX.also { e2.prevPosX = e1.prevPosX }
    e1.prevPosY = e2.prevPosY.also { e2.prevPosY = e1.prevPosY }
    e1.prevPosZ = e2.prevPosZ.also { e2.prevPosZ = e1.prevPosZ }
    e1.lastTickPosX = e2.lastTickPosX.also { e2.lastTickPosX = e1.lastTickPosX }
    e1.lastTickPosY = e2.lastTickPosY.also { e2.lastTickPosY = e1.lastTickPosY }
    e1.lastTickPosZ = e2.lastTickPosZ.also { e2.lastTickPosZ = e1.lastTickPosZ }

    e1.rotationYaw = e2.rotationYaw.also { e2.rotationYaw = e1.rotationYaw }
    e1.rotationPitch = e2.rotationPitch.also { e2.rotationPitch = e1.rotationPitch }
    e1.cameraYaw = e2.cameraYaw.also { e2.cameraYaw = e1.cameraYaw }
    //#if MC<11400
    e1.cameraPitch = e2.cameraPitch.also { e2.cameraPitch = e1.cameraPitch }
    //#endif

    e1.prevRotationYaw = e2.prevRotationYaw.also { e2.prevRotationYaw = e1.prevRotationYaw }
    e1.prevRotationPitch = e2.prevRotationPitch.also { e2.prevRotationPitch = e1.prevRotationPitch }
    e1.prevCameraYaw = e2.prevCameraYaw.also { e2.prevCameraYaw = e1.prevCameraYaw }
    //#if MC<11400
    e1.prevCameraPitch = e2.prevCameraPitch.also { e2.prevCameraPitch = e1.prevCameraPitch }
    //#endif

    e1.rotationYawHead = e2.rotationYawHead.also { e2.rotationYawHead = e1.rotationYawHead }
    e1.prevRotationYawHead = e2.prevRotationYawHead.also { e2.prevRotationYawHead = e1.prevRotationYawHead }
    e1.renderYawOffset = e2.renderYawOffset.also { e2.renderYawOffset = e1.renderYawOffset }
    e1.prevRenderYawOffset = e2.prevRenderYawOffset.also { e2.prevRenderYawOffset = e1.prevRenderYawOffset }

    e1.setPosition(e1.posX, e1.posY, e1.posZ)
    e2.setPosition(e2.posX, e2.posY, e2.posZ)

    //#if MC>=11400
    //$$ e1.motion = e2.motion.also { e2.motion = e1.motion }
    //#else
    e1.motionX = e2.motionX.also { e2.motionX = e1.motionX }
    e1.motionY = e2.motionY.also { e2.motionY = e1.motionY }
    e1.motionZ = e2.motionZ.also { e2.motionZ = e1.motionZ }
    //#endif

    e1.chasingPosX = e2.chasingPosX.also { e2.chasingPosX = e1.chasingPosX }
    e1.chasingPosY = e2.chasingPosY.also { e2.chasingPosY = e1.chasingPosY }
    e1.chasingPosZ = e2.chasingPosZ.also { e2.chasingPosZ = e1.chasingPosZ }

    e1.prevChasingPosX = e2.prevChasingPosX.also { e2.prevChasingPosX = e1.prevChasingPosX }
    e1.prevChasingPosY = e2.prevChasingPosY.also { e2.prevChasingPosY = e1.prevChasingPosY }
    e1.prevChasingPosZ = e2.prevChasingPosZ.also { e2.prevChasingPosZ = e1.prevChasingPosZ }
}

internal val <T> LazyLoadBase<T>.maybeValue get() =
    //#if MC>=11400
    //$$ // TODO https://github.com/ReplayMod/remap/issues/2
    //#if FABRIC>=1
    //$$ if ((this as AccLazyLoadBase).supplier == null) get() else null
    //#else
    //$$ if ((this as AccLazyLoadBase).supplier == null) value else null
    //#endif
    //#else
    if ((this as AccLazyLoadBase).isLoaded) value else null
    //#endif

internal fun clientSyncIgnoringView(task: () -> Unit) {
    val mc = Minecraft.getMinecraft()
    //#if MC>=11400
    //$$ // FIXME preprocessor should remap this one (it's merely missing yarn-mappings)
    //#if FABRIC>=1
    //$$ mc.method_18858(ViewDemuxingTaskQueue.ViewWrappedFutureTask({ null }, Runnable { task() }))
    //#else
    //$$ mc.enqueue(ViewDemuxingTaskQueue.ViewWrappedFutureTask({ null }, Runnable { task() }))
    //#endif
    //#else
    mc as AccMinecraft
    synchronized(mc.scheduledTasks) {
        mc.scheduledTasks.offer(ViewDemuxingTaskQueue.ViewWrappedFutureTask({
            null
        }, ListenableFutureTask.create(Executors.callable(task)).logFailure()))
    }
    //#endif
}
//#if MC<11400
internal fun <L : ListenableFuture<T>, T> L.logFailure(): L {
    Futures.addCallback(this, object : FutureCallback<T> {
        override fun onSuccess(result: T?) = Unit
        override fun onFailure(t: Throwable) {
            LOGGER.error("Failed future:", t)
        }
    })
    return this
}
//#endif
