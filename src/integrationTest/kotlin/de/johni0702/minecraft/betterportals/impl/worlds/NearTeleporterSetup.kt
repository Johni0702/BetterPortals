package de.johni0702.minecraft.betterportals.impl.worlds

import de.johni0702.minecraft.betterportals.common.portalManager
import de.johni0702.minecraft.betterportals.common.to3dMid
import de.johni0702.minecraft.betterportals.impl.*
import de.johni0702.minecraft.view.client.worldsManager
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
        mc.world.portalManager.loadedPortals.toList().forAll { it.remoteAgent.shouldNotBeNull() }
        mc.worldsManager!!.worlds shouldHaveSize 1 // same world
    }
}
