package de.johni0702.minecraft.betterportals.impl.worlds

import de.johni0702.minecraft.betterportals.common.plus
import de.johni0702.minecraft.betterportals.common.portalManager
import de.johni0702.minecraft.betterportals.common.to3dMid
import de.johni0702.minecraft.betterportals.impl.*
import io.kotlintest.Spec
import io.kotlintest.matchers.numerics.shouldBeExactly
import io.kotlintest.matchers.types.shouldNotBeNull
import io.kotlintest.seconds
import io.kotlintest.shouldBe
import io.kotlintest.until.until
import net.minecraft.init.Blocks
import net.minecraft.init.Items
import net.minecraft.util.math.BlockPos
import net.minecraft.world.WorldServer

open class SingleNetherPortalSetup : EmptyWorldSetup() {
    override fun beforeSpec(spec: Spec) {
        asMainThread {
            super.beforeSpec(spec)
            startFlying()
            buildDefaultPortal()
        }
    }

    open fun buildFrame(world: WorldServer, pos: BlockPos) {
        val frame = listOf(
                BlockPos( 2, 0, -2),
                BlockPos( 2, 0, -1),
                BlockPos( 2, 0,  0),
                BlockPos( 2, 0,  1),
                BlockPos( 2, 0,  2),
                BlockPos( 1, 0, -2),
                BlockPos( 1, 0,  2),
                BlockPos( 0, 0, -2),
                BlockPos( 0, 0,  2),
                BlockPos(-1, 0, -2),
                BlockPos(-1, 0,  2),
                BlockPos(-2, 0, -2),
                BlockPos(-2, 0, -1),
                BlockPos(-2, 0,  0),
                BlockPos(-2, 0,  1),
                BlockPos(-2, 0,  2)
        )
        frame.forEach {
            world.setBlockState(pos + it, Blocks.OBSIDIAN.defaultState)
        }
    }

    open fun buildDefaultPortal() {
        buildFrame(serverOverworld, BlockPos(0, 20, 0))
        buildFrame(serverNether, BlockPos(20, 30, 20))

        sendMessage("/give @p minecraft:flint_and_steel")

        tickServer()
        updateClient()
        mc.player.heldItemMainhand.item shouldBe Items.FLINT_AND_STEEL

        moveTo(BlockPos(0, 17, 0).to3dMid())

        val targetPos = BlockPos(1, 20, 0)
        lookAt(targetPos.to3dMid().addVector(0.5, 0.0, 0.0))
        tickClient()
        mc.objectMouseOver.blockPos shouldBe targetPos.east()

        mc.world.getBlockState(targetPos).block shouldBe Blocks.AIR
        mc.gameSettings.keyBindUseItem.trigger()
        tickClient()
        tickClient()
        mc.world.getBlockState(targetPos).block shouldBe Blocks.FIRE

        tickServer()
        serverOverworld.getBlockState(targetPos).block shouldBe Blocks.PORTAL
        serverOverworld.portalManager.loadedPortals.count() shouldBeExactly 1

        // Wait until remote portal has been created
        until(10.seconds, { serverOverworld.portalManager.loadedPortals.first().getRemoteAgent() != null }) {
            tickServer()
        }

        // Sync portal and remote world with client
        repeat(10) { tickServer() }
        updateClient()
        tickClient()

        mc.world.getBlockState(targetPos).block shouldBe Blocks.PORTAL
        mc.world.portalManager.loadedPortals.count() shouldBeExactly 1
        mc.world.portalManager.loadedPortals.first().getRemoteAgent().shouldNotBeNull()
    }
}