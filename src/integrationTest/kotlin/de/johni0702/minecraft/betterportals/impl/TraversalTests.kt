package de.johni0702.minecraft.betterportals.impl

import de.johni0702.minecraft.betterportals.common.dimensionId
import de.johni0702.minecraft.betterportals.common.tickPos
import de.johni0702.minecraft.betterportals.common.to3dMid
import de.johni0702.minecraft.betterportals.common.toDimensionId
import de.johni0702.minecraft.betterportals.impl.worlds.DoubleNetherPortalSetup
import de.johni0702.minecraft.betterportals.impl.worlds.SingleNetherPortalSetup
import io.kotlintest.extensions.TestListener
import io.kotlintest.matchers.doubles.shouldBeBetween
import io.kotlintest.matchers.doubles.shouldBeLessThanOrEqual
import io.kotlintest.shouldBe
import io.kotlintest.shouldNotBe
import io.kotlintest.specs.AnnotationSpec
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d

//#if MC>=11400
//#else
import de.johni0702.minecraft.betterportals.impl.worlds.NearTeleporterSetup
//#endif

open class SinglePortalTraversalTests : AnnotationSpec() {
    override fun listeners(): List<TestListener> = listOf(SingleNetherPortalSetup())

    private fun moveUpThroughPortal() {
        mc.world.dimensionId shouldBe 0.toDimensionId()
        mc.player.updateMovement { jump = true }
        repeat(10) {
            tickClient()
            tickServer()
        }
        mc.player.updateMovement { jump = false }
        tickClient()
        mc.world.dimensionId shouldBe (-1).toDimensionId()
    }

    private fun moveDownThroughPortal() {
        mc.world.dimensionId shouldBe (-1).toDimensionId()
        mc.player.updateMovement { sneak = true }
        repeat(10) {
            tickClient()
            tickServer()
        }
        mc.player.updateMovement { sneak = false }
        tickClient()
        mc.world.dimensionId shouldBe 0.toDimensionId()
    }

    /**
     * Simply move up through the portal and then back down through the portal.
     * @see testSimpleTraversalWithoutLag
     * @see testSimpleTraversalWithLag
     */
    private fun testSimpleTraversal(skipServerUpdate: Boolean = false) {
        moveUpThroughPortal()

        if (!skipServerUpdate) {
            updateClient()
            mc.world.dimensionId shouldBe (-1).toDimensionId()
        }

        // Move back down through portal
        moveDownThroughPortal()

        updateClient()
        mc.world.dimensionId shouldBe 0.toDimensionId()
    }

    @BeforeEach
    fun moveToStart() {
        moveTo(BlockPos(0, 17, 0).to3dMid())
        tickClient()
        tickServer()
        updateClient()
    }

    /**
     * Same as [testSimpleTraversal] with default arguments (kotlintest doesn't appear to fill those in)
     */
    @Test
    fun testSimpleTraversalWithoutLag() {
        testSimpleTraversal()
    }

    /**
     * Same as [testSimpleTraversal] but with network lag throughout the whole process.
     */
    @Test
    fun testSimpleTraversalWithLag() {
        testSimpleTraversal(skipServerUpdate = true)
    }

    /**
     * Player is teleported by the server after traversing both portals (nothing special should happen).
     */
    @Test
    fun testTeleportAfterTraversal() {
        moveUpThroughPortal()
        moveDownThroughPortal()
        sendTpCommand(mc.player.tickPos)

        updateClient()
        mc.world.dimensionId shouldBe 0.toDimensionId()
    }

    /**
     * Player is teleported by the server before traversing the portal for the second time.
     * They should be reset back into the nether (and will then have to move out again).
     */
    @Test
    fun testTeleportDuringTraversal() {
        moveUpThroughPortal()
        val tpPos = mc.player.tickPos
        sendTpCommand(tpPos)
        moveDownThroughPortal()

        mc.world.dimensionId shouldBe 0.toDimensionId()
        updateClient()
        mc.world.dimensionId shouldBe (-1).toDimensionId()
        mc.player.tickPos shouldBe tpPos

        moveDownThroughPortal()
        mc.world.dimensionId shouldBe 0.toDimensionId()
    }

    /**
     * Player is teleported by the server right at the start and locally traverses the portal once.
     * They should be reset back into the overworld.
     */
    @Test
    fun testTeleportBeforeTraversal() {
        val tpPos = mc.player.tickPos
        sendTpCommand(tpPos)
        moveUpThroughPortal()

        mc.world.dimensionId shouldBe (-1).toDimensionId()
        updateClient()
        mc.world.dimensionId shouldBe 0.toDimensionId()
        mc.player.tickPos shouldBe tpPos

        // Make sure it's not horribly broken in some way
        testSimpleTraversal()
    }

    /**
     * Player is teleported by the server right at the start and locally traverses the portal two times.
     * They should be reset back into the overworld.
     */
    @Test
    fun testTeleportBeforeNestedTraversal() {
        sendTpCommand(mc.player.tickPos)
        moveUpThroughPortal()
        moveDownThroughPortal()

        mc.world.dimensionId shouldBe 0.toDimensionId()
        updateClient()
        mc.world.dimensionId shouldBe 0.toDimensionId()

        // Make sure it's not horribly broken in some way
        testSimpleTraversal()
    }
}

open class SinglePortalWithSecondNearbyTraversalTest : SinglePortalTraversalTests() {
    override fun listeners(): List<TestListener> = listOf(DoubleNetherPortalSetup())
}

open class DoublePortalTraversalTests : AnnotationSpec() {
    override fun listeners(): List<TestListener> = listOf(DoubleNetherPortalSetup())

    private fun holdForTwentyTicks(movement: TestMovementInput.(Boolean) -> Unit) {
        mc.player.updateMovement { movement(true) }
        repeat(20) {
            tickClient()
            tickServer()
        }
        mc.player.updateMovement { movement(false) }
        tickClient()
    }

    /**
     * Simply move up through both portals and then back down through both portals.
     * @see testSimpleTraversalWithoutLag
     * @see testSimpleTraversalWithLag
     */
    private fun testSimpleTraversal(skipServerUpdateInNether: Boolean = false, skipServerUpdateInOverworld: Boolean = false) {
        mc.world.dimensionId shouldBe 0.toDimensionId()
        holdForTwentyTicks { jump = it }
        mc.world.dimensionId shouldBe (-1).toDimensionId()
        if (!skipServerUpdateInNether) {
            updateClient()
        }

        mc.world.dimensionId shouldBe (-1).toDimensionId()
        holdForTwentyTicks { jump = it }
        mc.world.dimensionId shouldBe 0.toDimensionId()
        if (!skipServerUpdateInOverworld) {
            updateClient()
        }

        mc.world.dimensionId shouldBe 0.toDimensionId()
        holdForTwentyTicks { sneak = it }
        mc.world.dimensionId shouldBe (-1).toDimensionId()
        if (!skipServerUpdateInNether) {
            updateClient()
        }

        mc.world.dimensionId shouldBe (-1).toDimensionId()
        holdForTwentyTicks { sneak = it }
        mc.world.dimensionId shouldBe 0.toDimensionId()
        updateClient()
        mc.world.dimensionId shouldBe 0.toDimensionId()
    }

    @BeforeEach
    fun moveToStart() {
        moveTo(BlockPos(0, 17, 0).to3dMid())
        tickClient()
        tickServer()
        updateClient()
    }

    /**
     * Same as [testSimpleTraversal] with default arguments (kotlintest doesn't appear to fill those in)
     */
    @Test
    fun testSimpleTraversalWithoutLag() {
        testSimpleTraversal()
    }

    /**
     * Same as [testSimpleTraversal] but with network lag while in the nether.
     */
    @Test
    fun testSimpleTraversalWithShortLag() {
        testSimpleTraversal(skipServerUpdateInOverworld = true)
    }

    /**
     * Same as [testSimpleTraversal] but with network lag throughout the whole process.
     */
    @Test
    fun testSimpleTraversalWithLag() {
        testSimpleTraversal(skipServerUpdateInNether = true, skipServerUpdateInOverworld = true)
    }
}

//#if MC<11400
open class NearTeleporterTraversalTests : AnnotationSpec() {
    override fun listeners(): List<TestListener> = listOf(NearTeleporterSetup(negativePowered = false))

    /**
     * Walk through the teleporter multiple times.
     */
    @Test
    fun testTraversal() {
        moveTo(Vec3d(0.5, 10.0, 0.5))
        lookAt(BlockPos(0, 10, 10).to3dMid())

        mc.player.updateMovement { forwardKeyDown = true; moveForward += 1 }
        var prevPos = mc.player.tickPos
        repeat(10) {
            tickClient()
            tickServer()
            mc.player.tickPos.x shouldBe prevPos.x
            mc.player.tickPos.y shouldBe prevPos.y
            mc.player.tickPos.z shouldNotBe prevPos.z
            mc.player.tickPos.z.shouldBeBetween(-2.0, 2.0, 0.0)
            prevPos = mc.player.tickPos
        }
        mc.player.updateMovement { forwardKeyDown = false }
        tickClient()

        prevPos = mc.player.tickPos
        tickServer()
        updateClient()
        mc.player.tickPos shouldBe prevPos
    }

    /**
     * Walk through the teleporter multiple times but in the other direction (which isn't active).
     */
    @Test
    fun testInactiveTraversal() {
        moveTo(Vec3d(0.5, 10.0, 0.5))
        lookAt(BlockPos(0, 10, -10).to3dMid())

        mc.player.updateMovement { forwardKeyDown = true; moveForward += 1 }
        var prevPos = mc.player.tickPos
        repeat(10) {
            tickClient()
            tickServer()
            mc.player.tickPos.x shouldBe prevPos.x
            mc.player.tickPos.y shouldBe prevPos.y
            mc.player.tickPos.z shouldBeLessThanOrEqual prevPos.z
            prevPos = mc.player.tickPos
        }
        mc.player.updateMovement { forwardKeyDown = false }
        tickClient()

        // Player should have walked against the back wall of the teleporter
        mc.player.tickPos.z.shouldBeBetween(-0.7, -0.7, 0.1)

        prevPos = mc.player.tickPos
        tickServer()
        updateClient()
        mc.player.tickPos shouldBe prevPos
    }
}
//#endif
