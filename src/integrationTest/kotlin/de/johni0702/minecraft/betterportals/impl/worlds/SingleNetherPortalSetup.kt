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

//#if MC>=11400
//$$ import net.minecraft.util.math.BlockRayTraceResult
//#endif

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
        buildFrame(serverNether, BlockPos(20, 80, 20))

        // Need something below the fire to be able to place it in 1.14
        // using (tall) grass because of its smaller hitbox and because it automatically breaks once on fire
        serverOverworld.setBlockState(BlockPos(1, 19, 0), Blocks.GRASS.defaultState)

        sendMessage("/give @p minecraft:flint_and_steel")

        tickServer()
        updateClient()
        mc.player.heldItemMainhand.item shouldBe Items.FLINT_AND_STEEL

        moveTo(BlockPos(0, 17, 0).to3dMid().addVector(0.0, 0.5, 0.0))

        val targetPos = BlockPos(1, 20, 0)
        lookAt(targetPos.to3dMid().addVector(0.5, 0.0, 0.0))
        tickClient()
        //#if MC>=11400
        //$$ (mc.objectMouseOver as BlockRayTraceResult).pos shouldBe targetPos.east()
        //#else
        mc.objectMouseOver.blockPos shouldBe targetPos.east()
        //#endif

        mc.world.getBlockState(targetPos).block shouldBe Blocks.AIR
        mc.gameSettings.keyBindUseItem.trigger()
        tickClient()
        tickClient()
        mc.world.getBlockState(targetPos).block shouldBe Blocks.FIRE

        tickServer()
        serverOverworld.getBlockState(targetPos).block shouldBe Blocks.PORTAL
        serverOverworld.portalManager.loadedPortals.count() shouldBeExactly 1

        // Wait until remote portal has been created
        until(10.seconds, { serverOverworld.portalManager.loadedPortals.first().remoteAgent != null }) {
            tickServer()
        }

        // Move player back to where all tests assume them to be (had to move them up for 1.14)
        moveTo(BlockPos(0, 17, 0).to3dMid())

        // Sync portal and remote world with client
        for (i in 0..10) {
            repeat(10) { tickServer() }
            updateClient()
            tickClient()
        }

        mc.world.getBlockState(targetPos).block shouldBe Blocks.PORTAL
        mc.world.portalManager.loadedPortals.count() shouldBeExactly 1
        val local = mc.world.portalManager.loadedPortals.first()
        val remote = local.remoteAgent
        remote.shouldNotBeNull()
        local.portal.localPosition.shouldBe(BlockPos(-1, 20, -1))
        remote.portal.localPosition.shouldBe(BlockPos(19, 80, 19))
    }
}