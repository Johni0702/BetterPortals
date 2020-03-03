package de.johni0702.minecraft.betterportals.impl

import de.johni0702.minecraft.betterportals.common.eyeOffset
import de.johni0702.minecraft.betterportals.common.forceAddEntity
import de.johni0702.minecraft.betterportals.common.forceRemoveEntity
import de.johni0702.minecraft.betterportals.common.minus
import de.johni0702.minecraft.view.client.render.OcclusionQuery
import io.kotlintest.matchers.boolean.shouldBeFalse
import io.kotlintest.shouldBe
import net.minecraft.client.multiplayer.WorldClient
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.entity.Render
import net.minecraft.client.renderer.entity.RenderManager
import net.minecraft.entity.Entity
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.ResourceLocation
import net.minecraft.util.math.Vec3d
import net.minecraft.world.World

//#if MC>=11400
//$$ import net.minecraft.entity.EntityClassification
//$$ import net.minecraft.entity.EntityType
//$$ import net.minecraft.entity.MoverType
//$$ import net.minecraft.network.IPacket
//$$ import net.minecraft.util.registry.Registry
//$$ import de.johni0702.minecraft.betterportals.common.entityTypeHolder
//#if FABRIC<1
//$$ import net.minecraftforge.fml.network.NetworkHooks
//#endif
//#endif

open class TestEntity(world: World) : Entity(
        //#if MC>=11400
        //$$ ENTITY_TYPE,
        //#endif
        world
) {
    var onUpdate: TestEntity.(() -> Unit) -> Unit = { it() }
    var shouldBeVisible = true
    var wasVisible = false
    var wasRendered = false

    //#if MC<11400
    init {
        setSize(1f, 3f)
    }
    //#endif

    //#if MC>=11400
    //#if FABRIC>=1
    //$$ override fun createSpawnPacket(): Packet<*> = theImpl.createSpawnPacket(this)
    //#else
    //$$ override fun createSpawnPacket(): IPacket<*> = NetworkHooks.getEntitySpawningPacket(this)
    //#endif
    //$$ fun move(type: MoverType, x: Double, y: Double, z: Double) {
    //$$     move(type, Vec3d(x, y, z))
    //$$ }
    //#else
    override fun getEyeHeight(): Float = 1.5f
    //#endif

    override fun writeEntityToNBT(compound: NBTTagCompound) = Unit
    override fun readEntityFromNBT(compound: NBTTagCompound) = Unit
    override fun entityInit() = Unit

    override fun onEntityUpdate() {
        onUpdate { super.onEntityUpdate() }
    }

    companion object {
        fun shouldBeVisible(world: WorldClient, pos: Vec3d) = shouldBe(world, pos, true)
        fun shouldNotBeVisible(world: WorldClient, pos: Vec3d) = shouldBe(world, pos, false)

        private fun shouldBe(world: WorldClient, eyePos: Vec3d, visible: Boolean) {
            val entity = TestEntity(world)
            with(eyePos - entity.eyeOffset) { entity.setPosition(x, y, z) }
            world.forceAddEntity(entity)

            entity.shouldBeVisible = visible
            render() // TODO we shouldn't have to render three times before it starts showing up..
            render()
            render()
            world.verifyTestEntityRenderResults() shouldBe 1

            world.forceRemoveEntity(entity)
        }

        //#if MC>=11400
        //$$ val ID = ResourceLocation("$MOD_ID:test")
        //$$ val ENTITY_TYPE: EntityType<TestEntity> by entityTypeHolder(ID)
        //#endif
    }
}

fun WorldClient.verifyTestEntityRenderResults(): Int {
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
    // TODO these three preprocessor statements should all be handled by remap, not sure why they aren't
    //#if FABRIC>=1
    //$$ override fun getTexture(p0: TestEntity?): Identifier? = null
    //#else
    override fun getEntityTexture(entity: TestEntity): ResourceLocation? = null
    //#endif

    //#if FABRIC>=1
    //$$ override fun render(entity: TestEntity, x: Double, y: Double, z: Double, entityYaw: Float, partialTicks: Float) = Unit
    //#else
    override fun doRender(entity: TestEntity, x: Double, y: Double, z: Double, entityYaw: Float, partialTicks: Float) = Unit
    //#endif
    override fun isMultipass(): Boolean = true
    //#if FABRIC>=1
    //$$ override fun renderSecondPass(entity: TestEntity, x: Double, y: Double, z: Double, entityYaw: Float, partialTicks: Float) {
    //#else
    override fun renderMultipass(entity: TestEntity, x: Double, y: Double, z: Double, entityYaw: Float, partialTicks: Float) {
    //#endif
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