package de.johni0702.minecraft.view.impl.client

import de.johni0702.minecraft.betterportals.common.dimensionId
import net.minecraft.client.Minecraft
import net.minecraft.client.entity.EntityPlayerSP
import net.minecraft.client.multiplayer.WorldClient
import net.minecraft.client.network.NetHandlerPlayClient
import net.minecraft.client.util.RecipeBookClient
import net.minecraft.stats.StatisticsManager

//#if MC>=11400
//$$ import net.minecraft.item.crafting.RecipeManager
//#else
import net.minecraft.block.material.Material
//#endif

internal class ViewEntity constructor(world: WorldClient, connection: NetHandlerPlayClient)
    : EntityPlayerSP(Minecraft.getMinecraft(), world, connection, StatisticsManager(), RecipeBookClient(
        //#if MC>=11400
        //$$ RecipeManager()
        //#endif
)) {

    init {
        // Negative ids cannot be used by the server
        // Fixed id should be fine as there'll only ever be one view entity per world (on the client at least)
        entityId = -4850

        // Entity doesn't set this by itself for some reason
        // (it's usually set in the JoinGame packet handler but views are created by their own packet)
        dimension = world.dimensionId
    }

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
}
