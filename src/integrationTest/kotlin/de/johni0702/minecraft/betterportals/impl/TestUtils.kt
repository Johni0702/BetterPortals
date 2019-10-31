package de.johni0702.minecraft.betterportals.impl

import de.johni0702.minecraft.betterportals.common.provideDelegate
import de.johni0702.minecraft.betterportals.impl.accessors.AccMinecraft
import de.johni0702.minecraft.view.client.render.RenderPassEvent
import io.kotlintest.matchers.collections.shouldBeEmpty
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
import net.minecraft.util.Util
import net.minecraft.util.math.BlockPos
import net.minecraft.world.GameType
import net.minecraft.world.WorldServer
import net.minecraft.world.WorldSettings
import net.minecraft.world.WorldType
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.common.FMLCommonHandler
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

val TEST_WORLD_SETTINGS = WorldSettings(0, GameType.CREATIVE, true, false, WorldType.FLAT).apply {
    enableCommands()
}

const val folderName = "test"
val server get() = mc.integratedServer!!
val serverOverworld: WorldServer get() = server.getWorld(0)
val serverNether: WorldServer get() = server.getWorld(-1)

fun launchServer() {
    mc.launchIntegratedServer(folderName, "test", TEST_WORLD_SETTINGS)
    with(server) {
        setOnlineMode(false)
        with(getWorld(0)) {
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

    updateClient()
    (mc as AccMinecraft).scheduledTasks.shouldBeEmpty()
    mc.world.shouldNotBeNull()
    mc.player.shouldNotBeNull()

    // Do not get movement input from keybindings
    mc.player.movementInput = MovementInput()
}

fun closeServer() {
    mc.world.sendQuittingDisconnectingPacket()
    mc.loadWorld(null)

    // MC just catches all exceptions but that still breaks our tests (starting 1.14 (or 13?) MC also clears the queue)
    (mc as AccMinecraft).scheduledTasks.clear()
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
            val syncPacket = SPacketCustomPayload("testsync", PacketBuffer(Unpooled.EMPTY_BUFFER))
            val connection = server.playerList.players[0].connection
            connection.sendPacket(syncPacket)
            clientSyncCond.await()
        }
    }
    synchronized((mc as AccMinecraft).scheduledTasks) {
        while (!(mc as AccMinecraft).scheduledTasks.isEmpty()) {
            Util.runTask((mc as AccMinecraft).scheduledTasks.poll(), LOGGER)
        }
    }
    mc.entityRenderer.getMouseOver(mc.renderPartialTicks)
    fastRender()
}

fun tickClient() {
    mc.runTick()
}

private object SkipRender {
    var registered by MinecraftForge.EVENT_BUS

    @SubscribeEvent
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

    FMLCommonHandler.instance().onRenderTickStart(partialTicks)
    mc.entityRenderer.updateCameraAndRender(partialTicks, 0)
    FMLCommonHandler.instance().onRenderTickEnd(partialTicks)

    (mc as AccMinecraft).framebuffer.unbindFramebuffer()
}

fun screenshot(name: String? = null): String =
        ScreenShotHelper.saveScreenshot(mc.mcDataDir, name, mc.displayWidth, mc.displayHeight, (mc as AccMinecraft).framebuffer).unformattedText

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
        val syncPacket = CPacketCustomPayload("testsync", PacketBuffer(Unpooled.EMPTY_BUFFER))
        val connection = mc.connection ?: return@withLock
        connection.sendPacket(syncPacket)
        serverSyncCond.await()
    }
    server.tick()
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

    override fun updatePlayerMoveState() {
        update()
    }
}

fun EntityPlayerSP.updateMovement(update: TestMovementInput.() -> Unit) {
    movementInput = TestMovementInput(movementInput, update)
}

fun KeyBinding.trigger() {
    val field = javaClass.getDeclaredField("pressTime").apply { isAccessible = true }
    field[this] = (field[this] as Int) + 1
}
