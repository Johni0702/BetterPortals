package de.johni0702.minecraft.betterportals.impl

import de.johni0702.minecraft.betterportals.common.eyeOffset
import de.johni0702.minecraft.betterportals.common.forceSpawnEntity
import de.johni0702.minecraft.betterportals.common.minus
import de.johni0702.minecraft.view.client.render.OcclusionQuery
import io.kotlintest.matchers.boolean.shouldBeFalse
import io.kotlintest.shouldBe
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.entity.Render
import net.minecraft.client.renderer.entity.RenderManager
import net.minecraft.entity.Entity
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.ResourceLocation
import net.minecraft.util.math.Vec3d
import net.minecraft.world.World

open class TestEntity(world: World) : Entity(world) {
    var onUpdate: TestEntity.(() -> Unit) -> Unit = { it() }
    var shouldBeVisible = true
    var wasVisible = false
    var wasRendered = false

    init {
        setSize(1f, 3f)
    }

    override fun getEyeHeight(): Float = 1.5f

    override fun writeEntityToNBT(compound: NBTTagCompound) = Unit
    override fun readEntityFromNBT(compound: NBTTagCompound) = Unit
    override fun entityInit() = Unit

    override fun onEntityUpdate() {
        onUpdate { super.onEntityUpdate() }
    }

    companion object {
        fun shouldBeVisible(world: World, pos: Vec3d) = shouldBe(world, pos, true)
        fun shouldNotBeVisible(world: World, pos: Vec3d) = shouldBe(world, pos, false)

        private fun shouldBe(world: World, eyePos: Vec3d, visible: Boolean) {
            val entity = TestEntity(world)
            with(eyePos - entity.eyeOffset) { entity.setPosition(x, y, z) }
            world.forceSpawnEntity(entity)

            entity.shouldBeVisible = visible
            render() // TODO we shouldn't have to render three times before it starts showing up..
            render()
            render()
            world.verifyTestEntityRenderResults() shouldBe 1

            world.removeEntityDangerously(entity)
        }
    }
}

fun World.verifyTestEntityRenderResults(): Int {
    var entities = 0
    loadedEntityList.forEach {
        if (it !is TestEntity) return@forEach
        entities++

        if (it.wasRendered) {
            it.wasVisible shouldBe it.shouldBeVisible
        } else {
            it.shouldBeVisible.shouldBeFalse()
        }

        it.wasRendered = false
    }
    return entities
}

class RenderTestEntity(renderManagerIn: RenderManager) : Render<TestEntity>(renderManagerIn) {
    override fun getEntityTexture(entity: TestEntity): ResourceLocation? = null

    // Multipass to ensure portals are rendered before the test entity because we rely on the occlusion query to function
    override fun doRender(entity: TestEntity, x: Double, y: Double, z: Double, entityYaw: Float, partialTicks: Float) = Unit
    override fun isMultipass(): Boolean = true
    override fun renderMultipass(entity: TestEntity, x: Double, y: Double, z: Double, entityYaw: Float, partialTicks: Float) {
        val occlusionQuery = OcclusionQuery()
        occlusionQuery.begin()

        // Intentionally grow the render bounding box because MC's entities aren't keeping to it either
        val box = entity.renderBoundingBox.grow(0.5)
        GlStateManager.pushMatrix()
        GlStateManager.disableCull()
        renderOffsetAABB(box, x - entity.lastTickPosX, y - entity.lastTickPosY, z - entity.lastTickPosZ)
        GlStateManager.enableCull()
        GlStateManager.popMatrix()

        occlusionQuery.end()
        occlusionQuery.awaitResult()
        entity.wasVisible = !occlusionQuery.occluded
        entity.wasRendered = true
    }
}