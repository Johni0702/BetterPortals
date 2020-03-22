package de.johni0702.minecraft.betterportals.impl.transition.server

import de.johni0702.minecraft.betterportals.common.DimensionId
import de.johni0702.minecraft.betterportals.common.dimensionId
import de.johni0702.minecraft.betterportals.common.theMovementFactor
import de.johni0702.minecraft.betterportals.common.tickPos
import de.johni0702.minecraft.betterportals.common.to3d
import de.johni0702.minecraft.betterportals.impl.transition.net.TransferToDimension
import de.johni0702.minecraft.betterportals.impl.transition.net.sendTo
import de.johni0702.minecraft.view.common.WorldsManager
import de.johni0702.minecraft.view.server.View
import de.johni0702.minecraft.view.server.worldsManager
import net.minecraft.entity.player.EntityPlayerMP

//#if FABRIC>=1
//$$ import net.fabricmc.fabric.impl.dimension.FabricDimensionInternals
//$$ import net.fabricmc.loader.api.FabricLoader
//#endif

//#if MC>=11400
//$$ import net.minecraft.util.math.BlockPos
//$$ import net.minecraft.util.math.Vec3d
//$$ import net.minecraft.world.dimension.DimensionType
//#else
import de.johni0702.minecraft.betterportals.common.toDimensionId
import de.johni0702.minecraft.betterportals.impl.transition.common.LOGGER
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
            dimension: DimensionId,
            //#if MC>=11400
            //$$ teleportTarget: Triple<Vec3d, Float, Float>?
            //#else
            teleporter: ITeleporter
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
        views.getOrPut(worldsManager, ::mutableMapOf)[id] = worldsManager.createView(oldWorld, player.tickPos, null)

        // Start transaction to allow the handler of TransferToDimension to update the camera in the target dimension before switching to it
        worldsManager.beginTransaction()

        // Inform the client of the transition to allow it to do a graphically fancy transition
        TransferToDimension(id).sendTo(player)

        // Transfer player to new world (calling code expects the player to have been transferred when the method returns)
        worldsManager.changeDimension(newWorld) {
            //#if MC>=11400
            //$$ if (teleportTarget != null) {
            //$$     val (pos, yaw, pitch) = teleportTarget
            //$$     with(pos) {
            //$$         setPositionAndRotation(x, y, z, yaw, pitch)
            //$$     }
            //$$     return@changeDimension
            //$$ }
            //#endif
            // Let the teleporter position the view entity
            // Based on PlayerList.transferEntityToWorld
            val newDim = newWorld.dimensionId
            val oldDim = oldWorld.dimensionId

            val moveFactor = oldWorld.theMovementFactor / newWorld.theMovementFactor
            val yaw = rotationYaw

            var posX = (posX * moveFactor).coerceIn(newWorld.worldBorder.minX() + 16.0, newWorld.worldBorder.maxX() - 16).toInt()
            var posZ = (posZ * moveFactor).coerceIn(newWorld.worldBorder.minZ() + 16.0, newWorld.worldBorder.maxZ() - 16).toInt()
            //#if MC>=11400
            //#if FABRIC>=1
            //$$ if (newDim == DimensionType.THE_END) {
            //$$     var spawn = BlockPos(posX, y.toInt(), posZ)
            //$$     var spawnYaw = yaw
            //$$     if (oldDim == DimensionType.OVERWORLD) {
            //$$         spawn = newWorld.forcedSpawnPoint!!
            //$$         spawnYaw = 90f
            //$$     }
            //$$     with(spawn.to3d()) {
            //$$         refreshPositionAndAngles(x, y, z, spawnYaw, 0f)
            //$$     }
            //$$     velocity = Vec3d.ZERO
            //$$ } else {
            //$$     val target = (if (FabricLoader.getInstance().isModLoaded("fabric-dimensions-v1")) ({
            //$$         FabricDimensionInternals.prepareDimensionalTeleportation(this)
            //$$         FabricDimensionInternals.tryFindPlacement(newWorld, null, 0.0, 0.0)
            //$$     }) else ({
            //$$         null
            //$$     }))()
            //$$     if (target != null) {
            //$$         this.velocity = target.velocity
            //$$         this.yaw = yaw + target.yaw
            //$$         with(target.pos) {
            //$$             updatePositionAndAngles(x, y, z, this@changeDimension.yaw, this@changeDimension.pitch)
            //$$         }
            //$$     } else if (!newWorld.portalForcer.usePortal(this, yaw)) {
            //$$         newWorld.portalForcer.createPortal(this)
            //$$         newWorld.portalForcer.usePortal(this, yaw)
            //$$     }
            //$$ }
            //#else
            //$$ TODO("1.14")
            //#endif
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