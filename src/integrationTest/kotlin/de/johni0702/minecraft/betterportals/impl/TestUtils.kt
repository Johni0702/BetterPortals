package de.johni0702.minecraft.betterportals.impl

import de.johni0702.minecraft.betterportals.common.toDimensionId
import de.johni0702.minecraft.betterportals.impl.accessors.AccMinecraft
import de.johni0702.minecraft.view.client.render.RenderPassEvent
import io.kotlintest.matchers.types.shouldNotBeNull
import io.kotlintest.milliseconds
import io.kotlintest.seconds
import io.kotlintest.until.fixedInterval
import io.kotlintest.until.until
import io.netty.buffer.Unpooled
import net.minecraft.client.entity.EntityPlayerSP
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.settings.KeyBinding
import net.minecraft.network.PacketBuffer
import net.minecraft.network.play.client.CPacketCustomPayload
import net.minecraft.network.play.server.SPacketCustomPayload
import net.minecraft.util.MovementInput
import net.minecraft.util.ScreenShotHelper
import net.minecraft.util.math.BlockPos
import net.minecraft.world.GameType
import net.minecraft.world.WorldServer
import net.minecraft.world.WorldSettings
import net.minecraft.world.WorldType
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

//#if FABRIC>=1
//$$ import de.johni0702.minecraft.betterportals.impl.accessors.AccServerWorld
//$$ import de.johni0702.minecraft.view.common.register
//#else
import de.johni0702.minecraft.betterportals.common.provideDelegate
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
//#endif

//#if MC>=11400
//$$ import io.kotlintest.shouldBe
//$$ import net.minecraft.client.world.ClientWorld
//$$ import net.minecraft.util.ResourceLocation
//$$ import net.minecraft.util.text.ITextComponent
//#if FABRIC<=0
//$$ import net.minecraftforge.fml.hooks.BasicEventHooks
//#endif
//$$ import java.util.concurrent.CompletableFuture
//$$ import kotlin.streams.toList
//#else
import io.kotlintest.matchers.collections.shouldBeEmpty
import net.minecraft.util.Util
import net.minecraftforge.fml.common.FMLCommonHandler
//#endif

//#if MC>=11400
//$$ interface IRender {
//$$     fun invokeRender();
//$$ }
//#endif

val TEST_WORLD_SETTINGS = WorldSettings(0, GameType.CREATIVE, true, false, WorldType.FLAT).apply {
    enableCommands()
}

const val folderName = "test"
val server get() = mc.integratedServer!!
val serverOverworld: WorldServer get() = server.getWorld(0.toDimensionId()!!)
val serverNether: WorldServer get() = server.getWorld((-1).toDimensionId()!!)

fun launchServer() {
    mc.launchIntegratedServer(folderName, "test", TEST_WORLD_SETTINGS)
    with(server) {
        setOnlineMode(false)
        with(getWorld(0.toDimensionId()!!)) {
            spawnPoint = BlockPos(0, 10, 0)
        }
    }

    // Forge handshake is a horrible mess
    until(10.seconds, fixedInterval(10.milliseconds), { server.playerList.players.isNotEmpty() }) {
        tickServer()
        updateClient(skipSync = true)
        tickClient()
    }

    // Fully complete handshake and initial world loading, 30 times should be plenty
    repeat(30) {
        updateClient()
        tickClient()
        tickServer()
    }

    //#if MC>=11400
    //$$ while (mc.isGamePaused) {
    //$$     (mc as IRender).invokeRender()
    //$$ }
    //#endif

    updateClient()
    //#if MC>=11400
    //$$ // TODO https://github.com/ReplayMod/remap/issues/10
    //#if FABRIC>=1
    //$$ mc.method_21684() shouldBe 0
    //#else
    //$$ mc.func_223704_be() shouldBe 0
    //#endif
    //#else
    (mc as AccMinecraft).scheduledTasks.shouldBeEmpty()
    //#endif
    mc.world.shouldNotBeNull()
    mc.player.shouldNotBeNull()

    // Do not get movement input from keybindings
    mc.player.movementInput = MovementInput()
}

fun closeServer() {
    mc.world.sendQuittingDisconnectingPacket()
    //#if MC>=11400
    //$$ mc.integratedServer?.close()
    //$$ mc.integratedServer?.initiateShutdown(true)
    //$$ mc.func_213254_o()
    //#else
    mc.loadWorld(null)
    //#endif

    // MC just catches all exceptions but that still breaks our tests (starting 1.14 (or 13?) MC also clears the queue)
    //#if MC<11400
    (mc as AccMinecraft).scheduledTasks.clear()
    //#endif
}

fun deleteWorld() {
    mc.saveLoader.deleteWorldDirectory(folderName)
}

private val clientSyncLock = ReentrantLock()
private val clientSyncCond = clientSyncLock.newCondition()

fun clientGotSync() {
    clientSyncLock.withLock { clientSyncCond.signal() }
}

fun updateClient(skipSync: Boolean = false) {
    if (!skipSync) {
        clientSyncLock.withLock {
            val syncPacket = SPacketCustomPayload(
                    //#if MC>=11400
                    //$$ ResourceLocation("betterportals", "testsync"),
                    //#else
                    "testsync",
                    //#endif
                    PacketBuffer(Unpooled.EMPTY_BUFFER)
            )
            val connection = server.playerList.players[0].connection
            connection.sendPacket(syncPacket)
            clientSyncCond.await()
        }
    }
    //#if MC>=11400
    //$$ // TODO preprocessor should handle this
    //#if FABRIC>=1
    //$$ mc.waitFor {
    //$$     mc.method_21684() == 0
    //$$ }
    //#else
    //$$ mc.driveUntil {
    //$$     mc.func_223704_be() == 0
    //$$ }
    //#endif
    //#else
    synchronized((mc as AccMinecraft).scheduledTasks) {
        while (!(mc as AccMinecraft).scheduledTasks.isEmpty()) {
            Util.runTask((mc as AccMinecraft).scheduledTasks.poll(), LOGGER)
        }
    }
    //#endif
    mc.entityRenderer.getMouseOver(mc.renderPartialTicks)
    fastRender()
}

fun tickClient() {
    mc.runTick()
}

private object SkipRender {
    //#if FABRIC>=1
    //$$ var registered = false
    //#else
    var registered by MinecraftForge.EVENT_BUS
    //#endif

    //#if FABRIC>=1
    //$$ init { RenderPassEvent.Prepare.EVENT.register { if (registered) preRenderPass(it) } }
    //#else
    @SubscribeEvent
    //#endif
    fun preRenderPass(event: RenderPassEvent.Prepare) {
        event.isCanceled = true
    }
}

fun fastRender() {
    SkipRender.registered = true
    try {
        render()
    } finally {
        SkipRender.registered = false
    }
}

fun render(partialTicks: Float = mc.renderPartialTicks) {
    (mc as AccMinecraft).framebuffer.bindFramebuffer(true)
    GlStateManager.enableTexture2D()

    //#if FABRIC>=1
    //$$ mc.gameRenderer.render(partialTicks, 0, true)
    //#else
    //#if MC>=11400
    //$$ BasicEventHooks.onRenderTickStart(partialTicks)
    //$$ mc.gameRenderer.updateCameraAndRender(partialTicks, 0, true)
    //$$ BasicEventHooks.onRenderTickEnd(partialTicks)
    //#else
    FMLCommonHandler.instance().onRenderTickStart(partialTicks)
    mc.entityRenderer.updateCameraAndRender(partialTicks, 0)
    FMLCommonHandler.instance().onRenderTickEnd(partialTicks)
    //#endif
    //#endif

    (mc as AccMinecraft).framebuffer.unbindFramebuffer()
}

//#if MC>=11400
//$$ fun screenshot(name: String? = null): String {
//$$     val future = CompletableFuture<ITextComponent>()
//$$     // TODO https://github.com/ReplayMod/remap/issues/10
    //#if FABRIC>=1
    //$$ ScreenshotUtils.method_1662(mc.runDirectory, name, mc.framebuffer.viewWidth, mc.framebuffer.viewHeight, (mc as AccMinecraft).framebuffer) {
    //#else
    //$$ ScreenShotHelper.saveScreenshot(mc.gameDir, name, mc.framebuffer.framebufferWidth, mc.framebuffer.framebufferHeight, (mc as AccMinecraft).framebuffer) {
    //#endif
//$$         future.complete(it)
//$$     }
//$$     return future.join().unformattedComponentText
//$$ }
//#else
fun screenshot(name: String? = null): String =
        ScreenShotHelper.saveScreenshot(mc.mcDataDir, name, mc.displayWidth, mc.displayHeight, (mc as AccMinecraft).framebuffer).unformattedText
//#endif

fun renderToScreenshot(name: String? = null): String {
    render()
    return screenshot(name)
}

private val serverSyncLock = ReentrantLock()
private val serverSyncCond = serverSyncLock.newCondition()

fun serverGotSync() {
    serverSyncLock.withLock { serverSyncCond.signal() }
}

fun tickServer() {
    serverSyncLock.withLock {
        val syncPacket = CPacketCustomPayload(
                //#if MC>=11400
                //$$ ResourceLocation("betterportals", "testsync"),
                //#else
                "testsync",
                //#endif
                PacketBuffer(Unpooled.EMPTY_BUFFER)
        )
        val connection = mc.connection ?: return@withLock
        connection.sendPacket(syncPacket)
        serverSyncCond.await()
    }
    //#if MC>=11400
    //#if FABRIC>=1
    //$$ server.waitFor {
    //$$     server.method_21684() == 0
    //$$ }
    //#else
    //$$ server.driveUntil {
    //$$     server.func_223704_be() == 0
    //$$ }
    //#endif
    //$$ server.tick { true }
    //#else
    server.tick()
    //#endif
}

class TestMovementInput(prevState: MovementInput, private val update: TestMovementInput.() -> Unit) : MovementInput() {
    init {
        moveStrafe = prevState.moveStrafe
        moveForward = prevState.moveForward
        forwardKeyDown = prevState.forwardKeyDown
        backKeyDown = prevState.backKeyDown
        leftKeyDown = prevState.leftKeyDown
        rightKeyDown = prevState.rightKeyDown
        jump = prevState.jump
        sneak = prevState.sneak
    }

    //#if MC>=11400
    //$$ override fun tick(slow: Boolean, noDampening: Boolean) {
    //$$     update()
    //$$ }
    //#else
    override fun updatePlayerMoveState() {
        update()
    }
    //#endif
}

fun EntityPlayerSP.updateMovement(update: TestMovementInput.() -> Unit) {
    movementInput = TestMovementInput(movementInput, update)
}

fun KeyBinding.trigger() {
    val field = javaClass.getDeclaredField(
            //#if FABRIC>=1
            //$$ "timesPressed"
            //#else
            "pressTime"
            //#endif
    ).apply { isAccessible = true }
    field[this] = (field[this] as Int) + 1
}

//#if MC>=11400
//#if FABRIC>=1
//$$ val ServerWorld.loadedEntityList get() = (this as AccServerWorld).entitiesById.values.toList()
//#else
//$$ val ServerWorld.loadedEntityList get() = entities.toList()
//#endif
//$$ val ClientWorld.loadedEntityList get() = allEntities.toList()
//#endif
