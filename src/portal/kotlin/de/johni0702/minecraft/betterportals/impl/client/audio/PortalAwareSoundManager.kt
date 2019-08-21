package de.johni0702.minecraft.betterportals.impl.client.audio

import de.johni0702.minecraft.betterportals.common.*
import de.johni0702.minecraft.view.client.worldsManager
import net.minecraft.client.Minecraft
import net.minecraft.client.audio.ISound
import net.minecraft.client.multiplayer.WorldClient
import net.minecraft.util.math.Vec3d

// Current shortcomings:
// - If the sound can be heard through more than one portal, one is chosen at random
// - Assumes portals to be stationary (sound system has only one listener, so this is hard to solve)
// - Recursion never goes through the same view twice (e.g. no infinite echos)
// - Passing through a portal will cause some sounds to suddenly switch direction. Really more of a shortcoming of
//     non-euclidean spaces with the lack of physics-based sound in MC (and games in general), not sure if we can do
//     anything about it.
// - Doesn't currently bother with repositioning sounds after initial calculation anyway (should be fine for
//     short sounds, don't care enough about long sounds)
// - Clamping sounds to portals is currently done in the most simple way possible (and not accurate at all)
// - New portals don't create new sound sources (limitation of sound system lack of source cloning)
// - Closing portals doesn't stop sounds
object PortalAwareSoundManager {
    lateinit var dropRemoteSounds: () -> Boolean
    private val mc = Minecraft.getMinecraft()
    private val viewForSound = mutableMapOf<ISound, WorldClient>()
    private val soundPaths = mutableMapOf<ISound, List<PortalAgent<*>>>()
    var listenerPos = Vec3d(0.0, 0.0, 0.0)

    fun recordView(sound: ISound): Boolean {
        val viewManager = mc.worldsManager ?: return true
        val world = viewForSound.computeIfAbsent(sound) { mc.world }
        return world == viewManager.player.world || !dropRemoteSounds()
    }

    fun beforePlay(sound: ISound) {
        val view = viewForSound[sound] ?: return
        val paths = findMainView(view, setOf(view)).toList()
        if (paths.isEmpty()) return
        soundPaths[sound] = paths.first()
    }

    private fun findMainView(from: WorldClient, visited: Set<WorldClient>): Set<List<PortalAgent<*>>> = if (from == mc.worldsManager?.player?.world) {
        setOf(emptyList())
    } else {
        val portalManager = from.portalManager
        portalManager.loadedPortals.flatMapTo(mutableSetOf<List<PortalAgent<*>>>()) { portalAgent ->
            val remoteWorld = portalAgent.remoteClientWorld ?: return@flatMapTo emptySet()
            if (remoteWorld in visited) {
                emptySet()
            } else {
                findMainView(remoteWorld, visited + setOf(remoteWorld)).map { listOf(portalAgent) + it }
            }
        }
    }

    val ISound.apparentPos get() = calcApparentLocation(
            Vec3d(xPosF.toDouble(), yPosF.toDouble(), zPosF.toDouble()),
            listenerPos,
            soundPaths[this] ?: emptyList()
    )

    private fun calcApparentLocation(pos: Vec3d, listener: Vec3d, portalStack: List<PortalAgent<*>>): Vec3d {
        val portalAgent = portalStack.lastOrNull() ?: return pos
        val portal = portalAgent.portal
        val apparentPos = with(portal) {
            calcApparentLocation(pos, listener.fromRemote().toLocal(), portalStack.dropLast(1)).fromLocal().toRemote()
        }

        // Clamp sound direction to portal surface (for sanity's sake, ignore the detailed bounding box)
        val portalBox = portal.remoteBoundingBox
        return if (portalBox.calculateIntercept(listener, apparentPos) != null) {
            // Direct line of sight (disregarding frame), good enough
            apparentPos
        } else {
            // The correct way would be minimizing the function which calculates the distance from sound to portal to
            // listener with the additional constraint that the solution be on the portal surface.
            // That's far too much work though, so we'll just assume that point to be in the center of the portal and
            // hope no one notices.
            val intersectionPoint = portalBox.center
            val distanceFromPortal = apparentPos.distanceTo(intersectionPoint)
            val direction = (intersectionPoint - listener).normalize()
            intersectionPoint + direction * distanceFromPortal
        }
    }
}
