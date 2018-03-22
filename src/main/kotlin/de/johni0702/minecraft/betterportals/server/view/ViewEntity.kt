package de.johni0702.minecraft.betterportals.server.view

import com.mojang.authlib.GameProfile
import de.johni0702.minecraft.betterportals.common.server
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.network.EnumPacketDirection
import net.minecraft.network.NetHandlerPlayServer
import net.minecraft.network.NetworkManager
import net.minecraft.network.play.client.CPacketClientSettings
import net.minecraft.server.management.PlayerInteractionManager
import net.minecraft.stats.StatBase
import net.minecraft.util.DamageSource
import net.minecraft.util.text.ITextComponent
import net.minecraft.world.GameType
import net.minecraft.world.World
import net.minecraft.world.WorldServer

internal class ViewEntity(world: WorldServer, profile: GameProfile, val parentConnection: NetHandlerPlayServer)
    : EntityPlayerMP(world.server, world, profile, PlayerInteractionManager(world)) {
    init {
        interactionManager.gameType = GameType.SPECTATOR
        connection = NetHandlerPlayServer(world.server, NetworkManager(EnumPacketDirection.SERVERBOUND), this)
    }

    override fun isSpectatedByPlayer(player: EntityPlayerMP): Boolean = false
    override fun canUseCommand(i: Int, s: String?): Boolean = false
    override fun sendStatusMessage(chatComponent: ITextComponent?, actionBar: Boolean) {}
    override fun sendMessage(component: ITextComponent?) {}
    override fun addStat(stat: StatBase?) {}
    override fun openGui(mod: Any?, modGuiId: Int, world: World?, x: Int, y: Int, z: Int) {}
    override fun isEntityInvulnerable(source: DamageSource?): Boolean = true
    override fun canAttackPlayer(player: EntityPlayer?): Boolean = false
    override fun onDeath(source: DamageSource?) {}
    override fun onUpdate() {}
    override fun changeDimension(dim: Int): Entity? = this
    override fun handleClientSettings(pkt: CPacketClientSettings?) {}
}
