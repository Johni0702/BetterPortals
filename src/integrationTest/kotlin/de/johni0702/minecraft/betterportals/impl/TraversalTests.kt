package de.johni0702.minecraft.betterportals.impl

import de.johni0702.minecraft.betterportals.common.pos
import de.johni0702.minecraft.betterportals.common.to3dMid
import de.johni0702.minecraft.betterportals.impl.worlds.SingleNetherPortalSetup
import io.kotlintest.extensions.TestListener
import io.kotlintest.shouldBe
import io.kotlintest.specs.AnnotationSpec
import net.minecraft.util.math.BlockPos

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