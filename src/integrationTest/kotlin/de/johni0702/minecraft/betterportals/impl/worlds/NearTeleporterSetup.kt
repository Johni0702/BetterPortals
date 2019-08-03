package de.johni0702.minecraft.betterportals.impl.worlds

import de.johni0702.minecraft.betterportals.common.portalManager
import de.johni0702.minecraft.betterportals.common.to3dMid
import de.johni0702.minecraft.betterportals.impl.*
import de.johni0702.minecraft.view.client.viewManager
import io.kotlintest.Spec
import io.kotlintest.inspectors.forAll
import io.kotlintest.matchers.collections.shouldHaveSize
import io.kotlintest.matchers.types.shouldNotBeNull
import io.kotlintest.shouldBe
import mekanism.api.transmitters.TransmissionType
import mekanism.common.MekanismBlocks
import mekanism.common.block.states.BlockStateBasic
import mekanism.common.block.states.BlockStateEnergyCube
import mekanism.common.block.states.BlockStateMachine
import mekanism.common.tier.EnergyCubeTier
import mekanism.common.tile.TileEntityEnergyCube
import mekanism.common.tile.TileEntityTeleporter
import net.minecraft.block.state.IBlockState
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import net.minecraft.world.WorldServer

val BlockStateBasic.BasicBlockType.defaultState: IBlockState
    get() = blockType.block.defaultState.withProperty(blockType.property, this)
val BlockStateMachine.MachineType.defaultState: IBlockState
        get() = typeBlock.block.defaultState.withProperty(typeBlock.property, this)

open class NearTeleporterSetup(
        private val positivePowered: Boolean = true,
        private val negativePowered: Boolean = true
) : EmptyWorldSetup() {
    override fun beforeSpec(spec: Spec) {
        asMainThread {
            super.beforeSpec(spec)
            startFlying()
            buildDefaultPortalPair()
        }
    }

    open fun buildFrame(world: WorldServer, pos: BlockPos, front: EnumFacing, powered: Boolean) {
        for (side in EnumFacing.HORIZONTALS) {
            if (side == front) continue
            for (i in -1..2) {
                world.setBlockState(pos.offset(side).up(i), BlockStateBasic.BasicBlockType.TELEPORTER_FRAME.defaultState)
            }
        }
        world.setBlockState(pos.up(2), BlockStateBasic.BasicBlockType.TELEPORTER_FRAME.defaultState)
        world.setBlockState(pos.down(), BlockStateMachine.MachineType.TELEPORTER.defaultState)
        if (powered) {
            world.setBlockState(pos.down(2),
                    MekanismBlocks.EnergyCube.defaultState
                            .withProperty(BlockStateEnergyCube.typeProperty, EnergyCubeTier.CREATIVE))
            (world.getTileEntity(pos.down(2)) as TileEntityEnergyCube).apply {
                energy = Double.MAX_VALUE
                config.setConfig(TransmissionType.ENERGY, 2, 2, 2, 2, 2, 2)
            }
        }
    }

    open fun buildDefaultPortalPair() {
        moveTo(BlockPos(0, 10, 0).to3dMid())
        tickClient()

        val posPositive = BlockPos(0, 10,  1)
        val posNegative = BlockPos(0, 10, -1)
        buildFrame(serverOverworld, posPositive, EnumFacing.NORTH, positivePowered)
        buildFrame(serverOverworld, posNegative, EnumFacing.SOUTH, negativePowered)

        tickServer()
        updateClient()

        val posTile = serverOverworld.getTileEntity(posPositive.down()) as TileEntityTeleporter
        val negTile = serverOverworld.getTileEntity(posNegative.down()) as TileEntityTeleporter
        posTile.security.ownerUUID = mc.player.uniqueID
        negTile.security.ownerUUID = mc.player.uniqueID
        posTile.setFrequency("1", true)
        negTile.setFrequency("1", true)

        tickServer()
        tickServer()
        posTile.status shouldBe (if (positivePowered) 1 else 4).toByte()
        negTile.status shouldBe (if (negativePowered) 1 else 4).toByte()
        serverOverworld.portalManager.loadedPortals.toList() shouldHaveSize 2

        updateClient()
        tickClient()
        mc.world.portalManager.loadedPortals.toList() shouldHaveSize 2
        mc.world.portalManager.loadedPortals.toList().forAll { it.getRemoteAgent().shouldNotBeNull() }
        mc.viewManager!!.views shouldHaveSize 1 // view sharing
    }
}

open class DistinctViewOnNearTeleporterSetup(
        positivePowered: Boolean = true,
        negativePowered: Boolean = true
) : NearTeleporterSetup(positivePowered, negativePowered) {
    override fun beforeSpec(spec: Spec) {
        asMainThread {
            super.beforeSpec(spec)
            makeDistinctViews()
        }
    }

    /**
     * Forces separate view to be used for the teleporter, i.e. not just the shared main view.
     * This is done by moving far away, thus unloading the teleporter,
     * then moving in until one end is loaded (but the other end is still too far away for sharing) and then finally
     * moving back to the original start position.
     * Note: This will break once we start sharing views on demand and not just on initial load.
     */
    open fun makeDistinctViews() {
        val loadDist = mc.gameSettings.renderDistanceChunks * 16.0

        mc.world.portalManager.loadedPortals.toList() shouldHaveSize 2
        mc.viewManager!!.views shouldHaveSize 1

        sendTpCommand(Vec3d(0.5, 10.0, loadDist + 24.0))
        repeat(10) {
            tickServer()
            updateClient()
            tickClient()
        }
        mc.world.portalManager.loadedPortals.toList() shouldHaveSize 0
        mc.viewManager!!.views shouldHaveSize 1

        // This should position the edge of loaded chunks in such a way that the portal with the positive z-coordinate
        // is loaded while the portal with the negative ones is not.
        sendTpCommand(Vec3d(0.5, 10.0, loadDist + 8.0))
        repeat(10) {
            tickServer()
            updateClient()
            tickClient()
        }
        mc.world.portalManager.loadedPortals.toList() shouldHaveSize 1
        mc.viewManager!!.views shouldHaveSize 2

        sendTpCommand(Vec3d(0.5, 10.0, 0.5))
        repeat(10) {
            tickServer()
            updateClient()
            tickClient()
        }
        mc.world.portalManager.loadedPortals.toList() shouldHaveSize 2
        mc.viewManager!!.views shouldHaveSize 2
    }
}
