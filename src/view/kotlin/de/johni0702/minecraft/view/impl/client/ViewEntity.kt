package de.johni0702.minecraft.view.impl.client

import de.johni0702.minecraft.view.impl.ClientViewAPIImpl
import net.minecraft.block.material.Material
import net.minecraft.client.Minecraft
import net.minecraft.client.entity.EntityPlayerSP
import net.minecraft.client.multiplayer.WorldClient
import net.minecraft.client.network.NetHandlerPlayClient
import net.minecraft.stats.RecipeBook
import net.minecraft.stats.StatisticsManager

internal class ViewEntity constructor(world: WorldClient, connection: NetHandlerPlayClient)
    : EntityPlayerSP(Minecraft.getMinecraft(), world, connection, StatisticsManager(), RecipeBook()) {

    init {
        // Negative ids cannot be used by the server
        // Fixed id should be fine as there'll only ever be one view entity per world (on the client at least)
        entityId = -4850

        // Entity doesn't set this by itself for some reason
        // (it's usually set in the JoinGame packet handler but views are created by their own packet)
        dimension = world.provider.dimension
    }

    override fun setPositionAndRotation(x: Double, y: Double, z: Double, yaw: Float, pitch: Float) {
        // If this method gets called while this view is the server's main view (i.e. the client went through a portal
        // but the server doesn't know yet), then the player has been teleported around on the server side and we need
        // to rewind the local portal usage. The server will ignored our UsePortal message because at the time it
        // receives the message we have an open teleport still awaiting confirmation (the one which caused this call).
        val viewManager = ClientViewAPIImpl.viewManagerImpl
        val serverMainView = viewManager.serverMainView
        if (serverMainView.camera == this) { // only when we are what the server sees as the main view
            viewManager.rewindMainView()
            // Forward pos/rot (camera of view will have been changed to the real player)
            serverMainView.camera.setPositionAndRotation(x, y, z, yaw, pitch)
        }
        super.setPositionAndRotation(x, y, z, yaw, pitch)
    }

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
