package de.johni0702.minecraft.view.impl.server

import com.mojang.authlib.GameProfile
import de.johni0702.minecraft.betterportals.common.server
import de.johni0702.minecraft.view.impl.IViewManagerHolder
import de.johni0702.minecraft.view.server.ServerViewManager
import de.johni0702.minecraft.view.server.viewManager
import net.minecraft.advancements.Advancement
import net.minecraft.advancements.PlayerAdvancements
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.network.EnumPacketDirection
import net.minecraft.network.NetHandlerPlayServer
import net.minecraft.network.NetworkManager
import net.minecraft.network.play.client.CPacketClientSettings
import net.minecraft.server.MinecraftServer
import net.minecraft.server.management.PlayerInteractionManager
import net.minecraft.stats.StatBase
import net.minecraft.stats.StatisticsManagerServer
import net.minecraft.util.DamageSource
import net.minecraft.util.text.ITextComponent
import net.minecraft.world.GameType
import net.minecraft.world.World
import net.minecraft.world.WorldServer
import java.io.File

internal class ViewEntity(world: WorldServer, profile: GameProfile, val parentConnection: NetHandlerPlayServer)
    : EntityPlayerMP(world.server, world, profile, PlayerInteractionManager(world)) {
    init {
        interactionManager.gameType = GameType.SPECTATOR
        connection = object : NetHandlerPlayServer(world.server, NetworkManager(EnumPacketDirection.SERVERBOUND), this), IViewManagerHolder {
            override val viewManager: ServerViewManager
                get() = parentConnection.viewManager
        }
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

@Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS") // all relevant methods have been overridden
internal class ViewStatsManager : StatisticsManagerServer(null, null) {
    override fun saveStatFile() = Unit
    override fun readStatFile() = Unit
    override fun unlockAchievement(playerIn: EntityPlayer, statIn: StatBase, p_150873_3_: Int) = Unit
    override fun markAllDirty() = Unit
    override fun sendStats(player: EntityPlayerMP) = Unit
    override fun increaseStat(player: EntityPlayer, stat: StatBase, amount: Int) = Unit
    override fun readStat(stat: StatBase): Int = 0
}

internal class ViewAdvancements(server: MinecraftServer, player: EntityPlayerMP)
    : PlayerAdvancements(server, File("invalidadvancementsfile"), player) {
    init {
        dispose()
    }

    override fun setSelectedTab(p_194220_1_: Advancement?) = Unit
    override fun save() = Unit
    override fun reload() = Unit
    override fun grantCriterion(p_192750_1_: Advancement, p_192750_2_: String): Boolean = false
    override fun flushDirty(p_192741_1_: EntityPlayerMP) = Unit
    override fun revokeCriterion(p_192744_1_: Advancement, p_192744_2_: String): Boolean = false
}
