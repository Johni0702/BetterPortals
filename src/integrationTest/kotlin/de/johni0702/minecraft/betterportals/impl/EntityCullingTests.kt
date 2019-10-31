package de.johni0702.minecraft.betterportals.impl

import de.johni0702.minecraft.betterportals.common.*
import de.johni0702.minecraft.betterportals.impl.worlds.SingleNetherPortalSetup
import de.johni0702.minecraft.view.client.worldsManager
import io.kotlintest.TestCaseConfig
import io.kotlintest.extensions.TestListener
import io.kotlintest.minutes
import io.kotlintest.shouldBe
import io.kotlintest.specs.AnnotationSpec
import net.minecraft.client.multiplayer.WorldClient
import net.minecraft.entity.MoverType
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d

class EntityCullingTests : AnnotationSpec() {
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

        val local = mc.world as WorldClient
        val remote = mc.worldsManager!!.worlds.find { it != local }!!

        local.provider.dimensionType shouldBe 0.toDimensionId()
        remote.provider.dimensionType shouldBe (-1).toDimensionId()

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
        // TODO https://github.com/Johni0702/BetterPortals/issues/230
        // TestEntity.shouldNotBeVisible(remote, remotePortal - offset + slightlyBelow)
        // Remote world, next to portal, top/far side
        TestEntity.shouldBeVisible(remote, remotePortal + offset + slightlyAbove)
    }

    @Test
    fun viewFromTop() {
        moveTo(Vec3d(-3.5, 24.0, 0.5) - mc.player.eyeOffset)
        lookAt(Vec3d(0.5, 20.5, 0.5))

        tickClient()

        val local = mc.world as WorldClient
        val remote = mc.worldsManager!!.worlds.find { it != local }!!

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
        // TODO https://github.com/Johni0702/BetterPortals/issues/230
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

        val local = mc.world as WorldClient
        val remote = mc.worldsManager!!.worlds.find { it != local }!!

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
        // TODO https://github.com/Johni0702/BetterPortals/issues/230
        // TestEntity.shouldNotBeVisible(remote, remotePortal + significantlyBelow)
        // Remote world, behind portal, top side
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

class EntityTraversalRenderTests : AnnotationSpec() {
    override fun listeners(): List<TestListener> = listOf(SingleNetherPortalSetup())

    @AfterEach
    fun removeTestEntities() {
        serverOverworld.loadedEntityList.filterIsInstance<TestEntity>().forEach {
            serverOverworld.forceRemoveEntity(it)
        }
        serverNether.loadedEntityList.filterIsInstance<TestEntity>().forEach {
            serverNether.forceRemoveEntity(it)
        }
    }

    private fun entityTraversal(startPos: Vec3d, direction: Double, shouldBeVisible: Boolean) {
        moveTo(Vec3d(0.5, 17.0, 0.5) - mc.player.eyeOffset)
        lookAt(Vec3d(0.5, 20.5, 0.5))

        tickClient()

        val local = mc.world as WorldClient
        val remote = mc.worldsManager!!.worlds.find { it != local }!!

        local.provider.dimensionType shouldBe 0.toDimensionId()
        remote.provider.dimensionType shouldBe (-1).toDimensionId()

        val overworldEntity = TestEntity(serverOverworld).apply {
            with(startPos - eyeOffset) { setPosition(x, y, z) }
        }
        serverOverworld.forceAddEntity(overworldEntity)
        tickServer()
        updateClient()
        tickClient()

        local.loadedEntityList.forEach { (it as? TestEntity)?.shouldBeVisible = shouldBeVisible }
        repeat(3) { render() }
        local.verifyTestEntityRenderResults() shouldBe 1

        repeat(10) {
            (serverOverworld.loadedEntityList + serverNether.loadedEntityList).forEach {
                (it as? TestEntity)?.onUpdate = { superOnEntityUpdate ->
                    superOnEntityUpdate()
                    move(MoverType.SELF, 0.0, direction, 0.0)
                }
            }

            tickServer()
            updateClient()
            tickClient()

            (local.loadedEntityList + remote.loadedEntityList).forEach {
                (it as? TestEntity)?.shouldBeVisible = shouldBeVisible
            }
            render(0.01f)
            local.verifyTestEntityRenderResults() + remote.verifyTestEntityRenderResults() shouldBe 1
            render(0.99f)
            local.verifyTestEntityRenderResults() + remote.verifyTestEntityRenderResults() shouldBe 1
        }

        render()
        remote.verifyTestEntityRenderResults() shouldBe 1

        repeat(10) {
            (serverOverworld.loadedEntityList + serverNether.loadedEntityList).forEach {
                (it as? TestEntity)?.onUpdate = { superOnEntityUpdate ->
                    superOnEntityUpdate()
                    move(MoverType.SELF, 0.0, -direction, 0.0)
                }
            }

            tickServer()
            updateClient()
            tickClient()

            (local.loadedEntityList + remote.loadedEntityList).forEach {
                (it as? TestEntity)?.shouldBeVisible = shouldBeVisible
            }
            render(0.01f)
            local.verifyTestEntityRenderResults() + remote.verifyTestEntityRenderResults() shouldBe 1
            render(0.99f)
            local.verifyTestEntityRenderResults() + remote.verifyTestEntityRenderResults() shouldBe 1
        }

        render()
        local.verifyTestEntityRenderResults() shouldBe 1
    }

    @Test
    fun visibleEntityTraversal() {
        entityTraversal(Vec3d(0.5, 19.4, 0.5), 0.2, true)
    }

    @Test
    fun hiddenEntityTraversal() {
        entityTraversal(Vec3d(0.5, 21.4, 0.5), -0.2, false)
    }
}
