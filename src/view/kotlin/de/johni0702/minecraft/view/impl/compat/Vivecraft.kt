package de.johni0702.minecraft.view.impl.compat

import de.johni0702.minecraft.betterportals.common.hasVivecraft
import de.johni0702.minecraft.betterportals.common.minus
import de.johni0702.minecraft.betterportals.common.plus
import de.johni0702.minecraft.betterportals.common.radians
import de.johni0702.minecraft.view.client.render.Camera
import de.johni0702.minecraft.view.client.render.DetermineRootPassEvent
import de.johni0702.minecraft.view.client.render.RenderPassEvent
import de.johni0702.minecraft.view.impl.LOGGER
import net.minecraft.client.Minecraft
import net.minecraft.util.math.Vec3d
import org.vivecraft.gameplay.OpenVRPlayer
import org.vivecraft.render.RenderPass

//#if FABRIC>=1
//$$ import de.johni0702.minecraft.view.common.register
//#else
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
//#endif

internal class VivecraftReflection {
    private val Minecraft_currentPass = Minecraft::class.java.getDeclaredField("currentPass")!!

    var currentPass: RenderPass
        get() = Minecraft_currentPass[Minecraft.getMinecraft()] as RenderPass
        set(value) { Minecraft_currentPass[Minecraft.getMinecraft()] = value }
}

internal val Vivecraft = if (hasVivecraft) VivecraftReflection() else null
// Note: do not shorten to `?.`, that'll require class-loading RenderPass
internal val viewRenderManagerSupported get() = Vivecraft == null || Vivecraft.currentPass != RenderPass.THIRD

private object VivecraftRoomRenderManager {
    lateinit var rootCamera: Camera // Camera corresponding to unmodified vrdata_world_renderer
    var orgOrigin: Vec3d = Vec3d.ZERO
    var orgRot: Float = 0f

    //#if FABRIC>=1
    //$$ init { DetermineRootPassEvent.EVENT.register { captureVanillaCamera(it) } }
    //#else
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    //#endif
    fun captureVanillaCamera(event: DetermineRootPassEvent) {
        rootCamera = event.camera
    }

    //#if FABRIC>=1
    //$$ init { RenderPassEvent.Before.EVENT.register { preRenderView(it) } }
    //#else
    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = false)
    //#endif
    fun preRenderView(event: RenderPassEvent.Before) {
        if (event.isCanceled) return
        val camera = event.renderPass.camera

        val vrData = OpenVRPlayer.get().vrdata_world_render
        orgOrigin = vrData.origin
        orgRot = vrData.rotation_radians
        val dYaw = (camera.viewRotation.y - rootCamera.viewRotation.y).radians.toFloat()
        vrData.origin = (vrData.origin - rootCamera.viewPosition).rotateYaw(dYaw) + camera.viewPosition
        vrData.rotation_radians += dYaw
    }

    //#if FABRIC>=1
    //$$ init { RenderPassEvent.After.EVENT.register { postRenderView(it) } }
    //#else
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    //#endif
    fun postRenderView(event: RenderPassEvent.After) {
        val vrData = OpenVRPlayer.get().vrdata_world_render
        vrData.origin = orgOrigin
        vrData.rotation_radians = orgRot
    }
}

internal fun registerVivecraftCompat() {
    Vivecraft ?: return

    LOGGER.info("Vivecraft detected. Enabling VR support. Disabling portals in third-person.")

    //#if FABRIC>=1
    //$$ VivecraftRoomRenderManager
    //#else
    MinecraftForge.EVENT_BUS.register(VivecraftRoomRenderManager)
    //#endif
}
