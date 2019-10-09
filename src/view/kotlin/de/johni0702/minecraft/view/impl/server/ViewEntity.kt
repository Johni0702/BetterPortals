package de.johni0702.minecraft.view.impl.server

import com.mojang.authlib.GameProfile
import de.johni0702.minecraft.betterportals.common.DimensionId
import de.johni0702.minecraft.betterportals.common.server
import de.johni0702.minecraft.view.impl.IWorldsManagerHolder
import de.johni0702.minecraft.view.impl.worldsManagerImpl
import io.netty.channel.embedded.EmbeddedChannel
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

//#if MC>=11400
//$$ import com.mojang.datafixers.DataFixer
//#endif

//#if MC>=11400
//$$ typealias StatAny = Stat<*>
//#else
typealias StatAny = StatBase
//#endif

internal class ViewEntity(
        world: WorldServer,
        profile: GameProfile,
        val parentConnection: NetHandlerPlayServer,
        val channel: EmbeddedChannel
) : EntityPlayerMP(world.server, world, profile, PlayerInteractionManager(world)) {
    init {
        interactionManager.gameType = GameType.SPECTATOR
        connection = object : NetHandlerPlayServer(world.server, NetworkManager(EnumPacketDirection.SERVERBOUND), this), IWorldsManagerHolder {
            override val worldsManager: ServerWorldsManagerImpl
                get() = parentConnection.worldsManagerImpl
        }
    }

    override fun isSpectatedByPlayer(player: EntityPlayerMP): Boolean = false
    override fun sendStatusMessage(chatComponent: ITextComponent?, actionBar: Boolean) {}
    override fun sendMessage(component: ITextComponent?) {}
    override fun addStat(stat: StatAny?) {}
    override fun isEntityInvulnerable(source: DamageSource?): Boolean = true
    override fun canAttackPlayer(player: EntityPlayer?): Boolean = false
    override fun onDeath(source: DamageSource?) {}
    override fun onUpdate() {}
    override fun changeDimension(dim: DimensionId): Entity? = this
    override fun handleClientSettings(pkt: CPacketClientSettings?) {}

    //#if MC<11400
    override fun canUseCommand(i: Int, s: String?): Boolean = false
    override fun openGui(mod: Any?, modGuiId: Int, world: World?, x: Int, y: Int, z: Int) {}
    //#endif
}

internal class ViewStatsManager(server: MinecraftServer) : StatisticsManagerServer(server, File(".")) {
    override fun saveStatFile() = Unit
    //#if MC>=11400
    //$$ override fun parseLocal(dataFixer: DataFixer, json: String) = Unit
    //#else
    override fun readStatFile() = Unit
    //#endif
    override fun unlockAchievement(playerIn: EntityPlayer, statIn: StatAny, p_150873_3_: Int) = Unit
    override fun markAllDirty() = Unit
    override fun sendStats(player: EntityPlayerMP) = Unit
    override fun increaseStat(player: EntityPlayer, stat: StatAny, amount: Int) = Unit
    override fun readStat(stat: StatAny): Int = 0
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
