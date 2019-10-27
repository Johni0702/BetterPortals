package de.johni0702.minecraft.betterportals.impl.accessors

import net.minecraft.client.renderer.ItemRenderer
import net.minecraft.item.ItemStack
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.gen.Accessor

@Mixin(ItemRenderer::class)
interface AccItemRenderer {
    @get:Accessor @set:Accessor
    var itemStackMainHand: ItemStack
    @get:Accessor @set:Accessor
    var itemStackOffHand: ItemStack
    @get:Accessor @set:Accessor
    var equippedProgressMainHand: Float
    @get:Accessor @set:Accessor
    var prevEquippedProgressMainHand: Float
    @get:Accessor @set:Accessor
    var equippedProgressOffHand: Float
    @get:Accessor @set:Accessor
    var prevEquippedProgressOffHand: Float
}