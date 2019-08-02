package de.johni0702.minecraft.betterportals.impl

import de.johni0702.minecraft.betterportals.common.degrees
import de.johni0702.minecraft.betterportals.common.minus
import io.kotlintest.shouldBe
import net.minecraft.util.math.Vec3d
import kotlin.math.asin
import kotlin.math.atan2

fun doubleJump() {
    tickClient()
    mc.player.updateMovement { jump = true }
    tickClient()
    mc.player.updateMovement { jump = false }
    tickClient()
    mc.player.updateMovement { jump = true }
    tickClient()
    mc.player.updateMovement { jump = false }
    // Wait a bit, otherwise we'll immediately trigger the fly-toggle code again
    repeat(10) {
        tickClient()
    }
}

fun startFlying() {
    mc.player.capabilities.isFlying shouldBe false

    mc.player.capabilities.allowFlying shouldBe true
    doubleJump()

    mc.player.capabilities.isFlying shouldBe true
}

fun stopFlying() {
    mc.player.capabilities.isFlying shouldBe true

    mc.player.capabilities.allowFlying shouldBe true
    doubleJump()

    mc.player.capabilities.isFlying shouldBe false
}

fun moveTo(pos: Vec3d) {
    mc.player.setPosition(pos.x, pos.y, pos.z)
}

fun lookAt(pos: Vec3d) {
    val dir = (pos - mc.player.getPositionEyes(1f)).normalize()
    mc.player.rotationYaw = -atan2(dir.x, dir.z).toFloat().degrees
    mc.player.rotationPitch = -asin(dir.y).toFloat().degrees
}

fun sendMessage(msg: String) {
    mc.player.sendChatMessage(msg)
}

fun sendTpCommand(dst: Vec3d) {
    sendMessage(with(dst) { "/tp $x $y $z" })
}
