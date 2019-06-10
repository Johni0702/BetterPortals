package de.johni0702.minecraft.view.impl.client.render

import com.mojang.authlib.GameProfile
import net.minecraft.block.material.Material
import net.minecraft.client.Minecraft
import net.minecraft.client.entity.EntityPlayerSP
import net.minecraft.client.multiplayer.WorldClient
import net.minecraft.client.network.NetHandlerPlayClient
import net.minecraft.stats.RecipeBook
import net.minecraft.stats.StatisticsManager
import java.util.*

private val viewCameraUUID = UUID.randomUUID()

@Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
private fun createDummyNetHandler() = NetHandlerPlayClient(
        Minecraft.getMinecraft(),
        null,
        null,
        GameProfile(viewCameraUUID, "camera")
)

internal class ViewCameraEntity constructor(world: WorldClient)
    : EntityPlayerSP(Minecraft.getMinecraft(), world, createDummyNetHandler(), StatisticsManager(), RecipeBook()) {

    init {
        // Negative ids cannot be used by the server
        // Fixed id should be fine as there'll only ever be one view camera entity per world at the same time
        entityId = -4851

        // Entity doesn't set this by itself for some reason
        // (it's usually set in the JoinGame packet handler but views are created by their own packet)
        dimension = world.provider.dimension
    }

    override fun getEyeHeight(): Float = eyeHeight
    override fun onUpdate() {}
    override fun isEntityInsideOpaqueBlock(): Boolean = false
    override fun isInsideOfMaterial(materialIn: Material): Boolean = false
    override fun isInLava(): Boolean = false
    override fun isInWater(): Boolean = false
    override fun isBurning(): Boolean = false
    override fun canBePushed(): Boolean = false
    override fun createRunningParticles() {}
    override fun canBeCollidedWith(): Boolean = false
    override fun isSpectator(): Boolean = true
    override fun shouldRenderInPass(pass: Int): Boolean = false
    override fun isInvisible(): Boolean = true
}
