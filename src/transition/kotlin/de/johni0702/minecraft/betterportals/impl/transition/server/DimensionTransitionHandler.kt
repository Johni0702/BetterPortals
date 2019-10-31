package de.johni0702.minecraft.betterportals.impl.transition.server

import de.johni0702.minecraft.betterportals.common.DimensionId
import de.johni0702.minecraft.betterportals.common.dimensionId
import de.johni0702.minecraft.betterportals.common.pos
import de.johni0702.minecraft.betterportals.common.to3d
import de.johni0702.minecraft.betterportals.common.toDimensionId
import de.johni0702.minecraft.betterportals.impl.transition.common.LOGGER
import de.johni0702.minecraft.betterportals.impl.transition.net.TransferToDimension
import de.johni0702.minecraft.betterportals.impl.transition.net.sendTo
import de.johni0702.minecraft.view.common.WorldsManager
import de.johni0702.minecraft.view.server.View
import de.johni0702.minecraft.view.server.worldsManager
import net.minecraft.entity.player.EntityPlayerMP

//#if MC>=11400
//#else
import net.minecraftforge.common.util.ITeleporter
//#endif

internal object DimensionTransitionHandler {
    val views = mutableMapOf<WorldsManager, MutableMap<Int, View>>()
    var enabled = true
    private var nextId = 0
    private val knownBadTeleporterClasses = listOf(
            // Stargate Network, see https://github.com/Johni0702/BetterPortals/issues/145
            "gcewing.sg.util.FakeTeleporter"
    )

    fun transferPlayerToDimension(
            player: EntityPlayerMP,
            dimension: DimensionId
            //#if MC<11400
            , teleporter: ITeleporter
            //#endif
    ): Boolean {
        if (!enabled) {
            return false
        }

        //#if MC<11400
        if (teleporter.javaClass.name in knownBadTeleporterClasses) {
            LOGGER.debug("Skipping fancy dimension transition because of bad teleporter class: {}", teleporter.javaClass)
            return false
        }
        //#endif

        val oldWorld = player.serverWorld
        val newWorld = player.mcServer!!.getWorld(dimension)
        val worldsManager = player.worldsManager
        val id = nextId++

        // Hold on to the old world until the client has finished the transition
        // (released in TransferToDimensionDone#Handler)
        views.getOrPut(worldsManager, ::mutableMapOf)[id] = worldsManager.createView(oldWorld, player.pos, null)

        // Start transaction to allow the handler of TransferToDimension to update the camera in the target dimension before switching to it
        worldsManager.beginTransaction()

        // Inform the client of the transition to allow it to do a graphically fancy transition
        TransferToDimension(id).sendTo(player)

        // Transfer player to new world (calling code expects the player to have been transferred when the method returns)
        worldsManager.changeDimension(newWorld) {
            // Let the teleporter position the view entity
            // Based on PlayerList.transferEntityToWorld
            val newDim = newWorld.dimensionId
            val oldDim = oldWorld.dimensionId

            val moveFactor = oldWorld.provider.movementFactor / newWorld.provider.movementFactor
            val yaw = rotationYaw

            var posX = (posX * moveFactor).coerceIn(newWorld.worldBorder.minX() + 16.0, newWorld.worldBorder.maxX() - 16).toInt()
            var posZ = (posZ * moveFactor).coerceIn(newWorld.worldBorder.minZ() + 16.0, newWorld.worldBorder.maxZ() - 16).toInt()
            //#if MC>=11400
            //$$ TODO("1.14")
            //#else
            if (newDim == 1.toDimensionId() && teleporter.isVanilla) {
                val spawn = (if (oldDim == 1.toDimensionId()) newWorld.spawnPoint else newWorld.spawnCoordinate)!!
                posX = spawn.x
                posZ = spawn.z
                with(spawn.to3d()) {
                    setLocationAndAngles(x, y, z, 90.0F, 0.0F)
                }
            }

            if (oldDim != 1.toDimensionId() || !teleporter.isVanilla) {
                posX = posX.coerceIn(-29999872, 29999872)
                posZ = posZ.coerceIn(-29999872, 29999872)

                setLocationAndAngles(posX.toDouble(), posY, posZ.toDouble(), rotationYaw, rotationPitch)
                teleporter.placeEntity(newWorld, this, yaw)
            }
            //#endif
        }

        worldsManager.endTransaction()

        // Finally send a poslook packet to have the client confirm the teleport (and to be able to discard any UsePortal messages until the confirmation)
        player.connection.setPlayerLocation(player.posX, player.posY, player.posZ, player.rotationYaw, player.rotationPitch)
        worldsManager.flushPackets()

        return true
    }
}