package de.johni0702.minecraft.betterportals.impl

import de.johni0702.minecraft.betterportals.common.eyeOffset
import de.johni0702.minecraft.betterportals.common.minus
import de.johni0702.minecraft.betterportals.common.plus
import de.johni0702.minecraft.betterportals.common.to3dMid
import de.johni0702.minecraft.betterportals.common.unaryMinus
import de.johni0702.minecraft.betterportals.impl.worlds.SingleNetherPortalSetup
import de.johni0702.minecraft.view.client.viewManager
import io.kotlintest.TestCaseConfig
import io.kotlintest.extensions.TestListener
import io.kotlintest.minutes
import io.kotlintest.shouldBe
import io.kotlintest.specs.AnnotationSpec
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import net.minecraft.world.World

class EntityRenderTests : AnnotationSpec() {
    private val localPortal = BlockPos(0, 20, 0).to3dMid()
    private val remotePortal = BlockPos(20, 80, 20).to3dMid()
    private val slightlyAbove = Vec3d(0.0, 0.1, 0.0)
    private val slightlyBelow = -slightlyAbove
    private val significantlyAbove = Vec3d(0.0, 1.6, 0.0)
    private val significantlyBelow = -significantlyAbove
    private val offset = Vec3d(3.0, 0.0, 0.0)

    // TODO remove once shouldBeVisible stops calling render() three times
    override val defaultTestCaseConfig: TestCaseConfig
        get() = super.defaultTestCaseConfig.copy(timeout = 2.minutes)

    override fun listeners(): List<TestListener> = listOf(SingleNetherPortalSetup())

    @BeforeEach
    fun moveToSide() {
        // Move far to the side of the portal, so we can then move into the specific position without hitting the portal
        moveTo(Vec3d(-10.0, 20.5, 0.5) - mc.player.eyeOffset)
    }

    @Test
    fun viewFromBelow() {
        moveTo(Vec3d(-3.5, 17.0, 0.5) - mc.player.eyeOffset)
        lookAt(Vec3d(0.5, 20.5, 0.5))

        tickClient()

        val local = mc.world as World
        val remote = mc.viewManager!!.views.map { it.camera.world }.find { it != local }!!

        local.provider.dimension shouldBe 0
        remote.provider.dimension shouldBe -1

        // Local world, inside portal, bottom/near side
        TestEntity.shouldBeVisible(local, localPortal + slightlyBelow)
        // Local world, inside portal, top/far side
        TestEntity.shouldNotBeVisible(local, localPortal + slightlyAbove)
        // Local world, in front of portal, bottom/near side
        TestEntity.shouldBeVisible(local, localPortal + significantlyBelow)
        // Local world, behind portal, top/far side
        TestEntity.shouldNotBeVisible(local, localPortal + significantlyAbove)
        // Local world, next to portal, bottom/near side
        TestEntity.shouldBeVisible(local, localPortal - offset + slightlyBelow)
        // Local world, next to portal, top/far side
        TestEntity.shouldBeVisible(local, localPortal + offset + slightlyAbove)

        // Remote world, inside portal, bottom/near side
        TestEntity.shouldNotBeVisible(remote, remotePortal + slightlyBelow)
        // Remote world, inside portal, top/far side
        TestEntity.shouldBeVisible(remote, remotePortal + slightlyAbove)
        // Remote world, in front of portal, bottom/near side
        TestEntity.shouldNotBeVisible(remote, remotePortal + significantlyBelow)
        // Remote world, behind portal, top/far side
        TestEntity.shouldBeVisible(remote, remotePortal + significantlyAbove)
        // Remote world, next to portal, bottom/near side
        // FIXME https://github.com/Johni0702/BetterPortals/issues/204
        // TestEntity.shouldNotBeVisible(remote, remotePortal - offset + slightlyBelow)
        // Remote world, next to portal, top/far side
        TestEntity.shouldBeVisible(remote, remotePortal + offset + slightlyAbove)
    }

    @Test
    fun viewFromTop() {
        moveTo(Vec3d(-3.5, 24.0, 0.5) - mc.player.eyeOffset)
        lookAt(Vec3d(0.5, 20.5, 0.5))

        tickClient()

        val local = mc.world as World
        val remote = mc.viewManager!!.views.map { it.camera.world }.find { it != local }!!

        // Local world, inside portal, bottom/far side
        TestEntity.shouldNotBeVisible(local, localPortal + slightlyBelow)
        // Local world, inside portal, top/near side
        TestEntity.shouldBeVisible(local, localPortal + slightlyAbove)
        // Local world, in front of portal, bottom/far side
        TestEntity.shouldNotBeVisible(local, localPortal + significantlyBelow)
        // Local world, behind portal, top/near side
        TestEntity.shouldBeVisible(local, localPortal + significantlyAbove)
        // Local world, next to portal, bottom/far side
        TestEntity.shouldBeVisible(local, localPortal + offset + slightlyBelow)
        // Local world, next to portal, top/near side
        TestEntity.shouldBeVisible(local, localPortal - offset + slightlyAbove)

        // Remote world, inside portal, bottom/far side
        TestEntity.shouldBeVisible(remote, remotePortal + slightlyBelow)
        // Remote world, inside portal, top/near side
        TestEntity.shouldNotBeVisible(remote, remotePortal + slightlyAbove)
        // Remote world, in front of portal, bottom/far side
        TestEntity.shouldBeVisible(remote, remotePortal + significantlyBelow)
        // Remote world, behind portal, top/near side
        TestEntity.shouldNotBeVisible(remote, remotePortal + significantlyAbove)
        // Remote world, next to portal, bottom/far side
        TestEntity.shouldBeVisible(remote, remotePortal + offset + slightlyBelow)
        // Remote world, next to portal, top/near side
        // FIXME https://github.com/Johni0702/BetterPortals/issues/204
        // TestEntity.shouldNotBeVisible(remote, remotePortal - offset + slightlyAbove)
    }

    @Test
    fun viewFromSideAndSlightlyBelow() {
        moveTo(Vec3d(-10.0, 20.1, 0.5) - mc.player.eyeOffset)
        viewFromSide()
    }

    @Test
    fun viewFromSideAndSlightlyAbove() {
        moveTo(Vec3d(-10.0, 20.9, 0.5) - mc.player.eyeOffset)
        viewFromSide()
    }

    private fun viewFromSide() {
        lookAt(Vec3d(0.5, 20.5, 0.5))

        tickClient()

        val local = mc.world as World
        val remote = mc.viewManager!!.views.map { it.camera.world }.find { it != local }!!

        // Local world, inside portal, bottom side
        TestEntity.shouldBeVisible(local, localPortal + slightlyBelow)
        // Local world, inside portal, top side
        TestEntity.shouldBeVisible(local, localPortal + slightlyAbove)
        // Local world, in front of portal, bottom side
        TestEntity.shouldBeVisible(local, localPortal + significantlyBelow)
        // Local world, behind portal, top side
        TestEntity.shouldBeVisible(local, localPortal + significantlyAbove)
        // Local world, next to portal, bottom side, near
        TestEntity.shouldBeVisible(local, localPortal - offset + slightlyBelow)
        // Local world, next to portal, top side, near
        TestEntity.shouldBeVisible(local, localPortal - offset + slightlyAbove)
        // Local world, next to portal, bottom side, far
        TestEntity.shouldBeVisible(local, localPortal + offset + slightlyBelow)
        // Local world, next to portal, top side, far
        TestEntity.shouldBeVisible(local, localPortal + offset + slightlyAbove)

        // Remote world, inside portal, bottom side
        // FIXME https://github.com/Johni0702/BetterPortals/issues/75
        // TestEntity.shouldBeVisible(remote, remotePortal + slightlyBelow)
        // Remote world, inside portal, top side
        // FIXME https://github.com/Johni0702/BetterPortals/issues/75
        // TestEntity.shouldBeVisible(remote, remotePortal + slightlyAbove)
        // Remote world, in front of portal, bottom side
        // FIXME https://github.com/Johni0702/BetterPortals/issues/204
        // TODO https://github.com/Johni0702/BetterPortals/issues/230
        // TestEntity.shouldNotBeVisible(remote, remotePortal + significantlyBelow)
        // Remote world, behind portal, top side
        // FIXME https://github.com/Johni0702/BetterPortals/issues/204
        // TODO https://github.com/Johni0702/BetterPortals/issues/230
        // TestEntity.shouldNotBeVisible(remote, remotePortal + significantlyAbove)
        // Remote world, next to portal, bottom side, near
        TestEntity.shouldNotBeVisible(remote, remotePortal - offset + slightlyBelow)
        // Remote world, next to portal, top side, near
        TestEntity.shouldNotBeVisible(remote, remotePortal - offset + slightlyAbove)
        // Remote world, next to portal, bottom side, far
        TestEntity.shouldNotBeVisible(remote, remotePortal + offset + slightlyBelow)
        // Remote world, next to portal, top side, far
        TestEntity.shouldNotBeVisible(remote, remotePortal + offset + slightlyAbove)
    }
}
