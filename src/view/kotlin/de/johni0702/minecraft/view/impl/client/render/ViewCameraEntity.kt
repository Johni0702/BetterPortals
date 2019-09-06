package de.johni0702.minecraft.view.impl.client.render

import com.mojang.authlib.GameProfile
import de.johni0702.minecraft.view.client.ClientViewAPI
import net.minecraft.block.material.Material
import net.minecraft.client.Minecraft
import net.minecraft.client.entity.EntityPlayerSP
import net.minecraft.client.multiplayer.WorldClient
import net.minecraft.client.network.NetHandlerPlayClient
import net.minecraft.client.util.RecipeBookClient
import net.minecraft.potion.Potion
import net.minecraft.potion.PotionEffect
import net.minecraft.stats.StatisticsManager
import java.util.*

//#if MC>=11400
//$$ import net.minecraft.entity.EntitySize
//$$ import net.minecraft.entity.Pose
//$$ import net.minecraft.item.crafting.RecipeManager
//#else
//#endif

private val viewCameraUUID = UUID.randomUUID()

@Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
private fun createDummyNetHandler() = NetHandlerPlayClient(
        Minecraft.getMinecraft(),
        null,
        null,
        GameProfile(viewCameraUUID, "camera")
)

internal class ViewCameraEntity constructor(world: WorldClient)
    : EntityPlayerSP(Minecraft.getMinecraft(), world, createDummyNetHandler(), StatisticsManager(), RecipeBookClient(
        //#if MC>=11400
        //$$ RecipeManager()
        //#endif
)) {

    init {
        // Negative ids cannot be used by the server
        // Fixed id should be fine as there'll only ever be one view camera entity per world at the same time
        entityId = -4851

        // Entity doesn't set this by itself for some reason
        // (it's usually set in the JoinGame packet handler but views are created by their own packet)
        dimension = world.provider.dimension
    }

    //#if MC>=11400
    //$$ var eyeHeightOverwrite = 0.toFloat()
    //$$ override fun getStandingEyeHeight(pose: Pose, size: EntitySize): Float = eyeHeightOverwrite
    //$$ override fun getEyeHeight(pose: Pose): Float = eyeHeight
    //#else
    override fun getEyeHeight(): Float = eyeHeight
    //#endif
    override fun onUpdate() {}
    override fun isEntityInsideOpaqueBlock(): Boolean = false
    override fun isInLava(): Boolean = false
    override fun isInWater(): Boolean = false
    override fun isBurning(): Boolean = false
    override fun canBePushed(): Boolean = false
    override fun createRunningParticles() {}
    override fun canBeCollidedWith(): Boolean = false
    override fun isSpectator(): Boolean = true
    override fun isInvisible(): Boolean = true

    //#if MC>=11400
    //$$ override fun isInRangeToRender3d(x: Double, y: Double, z: Double): Boolean = false
    //$$ override fun isInRangeToRenderDist(dist: Double): Boolean = false
    //#else
    override fun isInsideOfMaterial(materialIn: Material): Boolean = false
    override fun shouldRenderInPass(pass: Int): Boolean = false
    //#endif

    private val actualPlayer = ClientViewAPI.instance.getWorldsManager(mc)?.player
    override fun getActivePotionMap(): MutableMap<Potion, PotionEffect> = actualPlayer?.activePotionMap ?: mutableMapOf()
    override fun getActivePotionEffects(): MutableCollection<PotionEffect> = actualPlayer?.activePotionEffects ?: mutableListOf()
    override fun getActivePotionEffect(potionIn: Potion): PotionEffect? = actualPlayer?.getActivePotionEffect(potionIn)
    override fun isPotionActive(potionIn: Potion): Boolean = actualPlayer?.isPotionActive(potionIn) ?: false
}
