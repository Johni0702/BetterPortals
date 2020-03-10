//#if MC<11400
package de.johni0702.minecraft.betterportals.impl.tf.common.entity

import de.johni0702.minecraft.betterportals.common.FinitePortal
import de.johni0702.minecraft.betterportals.common.entity.OneWayPortalEntity
import de.johni0702.minecraft.betterportals.impl.tf.BPTwilightForestMod.Companion.PORTAL_CONFIG
import net.minecraft.block.Block
import net.minecraft.init.Blocks
import net.minecraft.world.World
import twilightforest.block.TFBlocks

class TFPortalEntity(isTailEnd: Boolean, world: World, portal: FinitePortal) : OneWayPortalEntity(isTailEnd, world, portal, PORTAL_CONFIG) {
    @Suppress("unused")
    constructor(world: World) : this(false, world, FinitePortal.DUMMY)

    override val portalFrameBlock: Block get() = Blocks.GRASS

    override fun onUpdate() {
        super.onUpdate()
        // Prevent legacy portal blocks from rendering on the client when we have a better portal
        if (world.isRemote && !isDead) {
            // Cannot replace with AIR because we still need the block light
            val replacementState = Blocks.PORTAL.defaultState
            portal.localBlocks.forEach {
                if (world.getBlockState(it).block == TFBlocks.twilight_portal) {
                    world.setBlockState(it, replacementState)
                }
            }
        }
    }

    override fun removePortal() {
        if (!isTailEnd) {
            portal.localBlocks.forEach { world.setBlockState(it, Blocks.WATER.defaultState) }
        }
    }
}
//#endif
