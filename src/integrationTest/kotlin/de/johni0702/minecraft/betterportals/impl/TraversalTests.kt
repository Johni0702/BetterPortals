package de.johni0702.minecraft.betterportals.impl

import de.johni0702.minecraft.betterportals.common.pos
import de.johni0702.minecraft.betterportals.common.to3dMid
import de.johni0702.minecraft.betterportals.impl.worlds.NearTeleporterSetup
import de.johni0702.minecraft.betterportals.impl.worlds.SingleNetherPortalSetup
import io.kotlintest.extensions.TestListener
import io.kotlintest.matchers.doubles.shouldBeBetween
import io.kotlintest.matchers.doubles.shouldBeLessThanOrEqual
import io.kotlintest.shouldBe
import io.kotlintest.shouldNotBe
import io.kotlintest.specs.AnnotationSpec
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d

class SinglePortalTraversalTests : AnnotationSpec() {
    override fun listeners(): List<TestListener> = listOf(SingleNetherPortalSetup())

    private fun moveUpThroughPortal() {
        mc.world.provider.dimension shouldBe 0
        mc.player.updateMovement { jump = true }
        repeat(10) {
            tickClient()
            tickServer()
        }
        mc.player.updateMovement { jump = false }
        tickClient()
        mc.world.provider.dimension shouldBe -1
    }

    private fun moveDownThroughPortal() {
        mc.world.provider.dimension shouldBe -1
        mc.player.updateMovement { sneak = true }
        repeat(10) {
            tickClient()
            tickServer()
        }
        mc.player.updateMovement { sneak = false }
        tickClient()
        mc.world.provider.dimension shouldBe 0
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
            mc.world.provider.dimension shouldBe -1
        }

        // Move back down through portal
        moveDownThroughPortal()

        updateClient()
        mc.world.provider.dimension shouldBe 0
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
        sendTpCommand(mc.player.pos)

        updateClient()
        mc.world.provider.dimension shouldBe 0
    }

    /**
     * Player is teleported by the server before traversing the portal for the second time.
     * They should be reset back into the nether (and will then have to move out again).
     */
    @Test
    fun testTeleportDuringTraversal() {
        moveUpThroughPortal()
        val tpPos = mc.player.pos
        sendTpCommand(tpPos)
        moveDownThroughPortal()

        mc.world.provider.dimension shouldBe 0
        updateClient()
        mc.world.provider.dimension shouldBe -1
        mc.player.pos shouldBe tpPos

        moveDownThroughPortal()
        mc.world.provider.dimension shouldBe 0
    }

    /**
     * Player is teleported by the server right at the start and locally traverses the portal once.
     * They should be reset back into the overworld.
     */
    @Test
    fun testTeleportBeforeTraversal() {
        val tpPos = mc.player.pos
        sendTpCommand(tpPos)
        moveUpThroughPortal()

        mc.world.provider.dimension shouldBe -1
        updateClient()
        mc.world.provider.dimension shouldBe 0
        mc.player.pos shouldBe tpPos

        // Make sure it's not horribly broken in some way
        testSimpleTraversal()
    }

    /**
     * Player is teleported by the server right at the start and locally traverses the portal two times.
     * They should be reset back into the overworld.
     */
    @Test
    fun testTeleportBeforeNestedTraversal() {
        sendTpCommand(mc.player.pos)
        moveUpThroughPortal()
        moveDownThroughPortal()

        mc.world.provider.dimension shouldBe 0
        updateClient()
        mc.world.provider.dimension shouldBe 0

        // Make sure it's not horribly broken in some way
        testSimpleTraversal()
    }
}

class NearTeleporterTraversalTests : AnnotationSpec() {
    override fun listeners(): List<TestListener> = listOf(NearTeleporterSetup(negativePowered = false))

    /**
     * Walk through the teleporter multiple times.
     */
    @Test
    fun testTraversal() {
        moveTo(Vec3d(0.5, 10.0, 0.5))
        lookAt(BlockPos(0, 10, 10).to3dMid())

        mc.player.updateMovement { forwardKeyDown = true; moveForward += 1 }
        var prevPos = mc.player.pos
        repeat(10) {
            tickClient()
            tickServer()
            mc.player.pos.x shouldBe prevPos.x
            mc.player.pos.y shouldBe prevPos.y
            mc.player.pos.z shouldNotBe prevPos.z
            mc.player.pos.z.shouldBeBetween(-2.0, 2.0, 0.0)
            prevPos = mc.player.pos
        }
        mc.player.updateMovement { forwardKeyDown = false }
        tickClient()

        prevPos = mc.player.pos
        tickServer()
        updateClient()
        mc.player.pos shouldBe prevPos
    }

    /**
     * Walk through the teleporter multiple times but in the other direction (which isn't active).
     */
    @Test
    fun testInactiveTraversal() {
        moveTo(Vec3d(0.5, 10.0, 0.5))
        lookAt(BlockPos(0, 10, -10).to3dMid())

        mc.player.updateMovement { forwardKeyDown = true; moveForward += 1 }
        var prevPos = mc.player.pos
        repeat(10) {
            tickClient()
            tickServer()
            mc.player.pos.x shouldBe prevPos.x
            mc.player.pos.y shouldBe prevPos.y
            mc.player.pos.z shouldBeLessThanOrEqual prevPos.z
            prevPos = mc.player.pos
        }
        mc.player.updateMovement { forwardKeyDown = false }
        tickClient()

        // Player should have walked against the back wall of the teleporter
        mc.player.pos.z.shouldBeBetween(-0.7, -0.7, 0.1)

        prevPos = mc.player.pos
        tickServer()
        updateClient()
        mc.player.pos shouldBe prevPos
    }
}