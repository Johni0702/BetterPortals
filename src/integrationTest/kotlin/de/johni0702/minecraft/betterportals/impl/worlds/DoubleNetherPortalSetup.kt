package de.johni0702.minecraft.betterportals.impl.worlds

import de.johni0702.minecraft.betterportals.common.portalManager
import de.johni0702.minecraft.betterportals.impl.*
import io.kotlintest.Spec
import io.kotlintest.matchers.numerics.shouldBeExactly
import io.kotlintest.matchers.types.shouldNotBeNull
import io.kotlintest.seconds
import io.kotlintest.shouldBe
import io.kotlintest.until.until
import net.minecraft.init.Blocks
import net.minecraft.util.math.BlockPos

open class DoubleNetherPortalSetup : SingleNetherPortalSetup() {
    override fun beforeSpec(spec: Spec) {
        asMainThread {
            super.beforeSpec(spec)
            buildSecondPortal()
        }
    }

    open fun buildSecondPortal() {
        val overworldPos = BlockPos(7 * 16, 30, 7 * 16) // close but still inside view limit
        buildFrame(serverOverworld, overworldPos)
        buildFrame(serverNether, BlockPos(20, 90, 20))

        tickServer()
        updateClient()

        serverOverworld.getBlockState(overworldPos).block shouldBe Blocks.AIR
        Blocks.PORTAL.trySpawnPortal(serverOverworld, overworldPos)
        serverOverworld.getBlockState(overworldPos).block shouldBe Blocks.PORTAL
        serverOverworld.portalManager.loadedPortals.count() shouldBeExactly 2

        // Wait until remote portal has been created
        until(10.seconds, { serverOverworld.portalManager.loadedPortals.all { it.getRemoteAgent() != null } }) {
            tickServer()
        }

        val first = mc.world.portalManager.loadedPortals.first()

        // Sync portal and remote world with client
        repeat(10) { tickServer() }
        updateClient()
        tickClient()

        mc.world.getBlockState(overworldPos).block shouldBe Blocks.PORTAL
        mc.world.portalManager.loadedPortals.count() shouldBeExactly 2
        val local = mc.world.portalManager.loadedPortals.find { it != first}!!
        val remote = local.getRemoteAgent()
        remote.shouldNotBeNull()
        local.portal.localPosition.shouldBe(overworldPos.add(-1, 0, -1))
        remote.portal.localPosition.shouldBe(BlockPos(19, 90, 19))
    }
}