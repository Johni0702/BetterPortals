package de.johni0702.minecraft.betterportals.server.view

import de.johni0702.minecraft.betterportals.MOD_ID
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.util.ResourceLocation
import net.minecraftforge.event.AttachCapabilitiesEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

internal class AttachServerViewManagerCapability {
    @SubscribeEvent
    fun onAttach(event: AttachCapabilitiesEvent<Entity>) {
        val entity = event.`object`
        if (entity is EntityPlayerMP) {
            event.addCapability(ResourceLocation(MOD_ID, "view_manager"), ServerViewManager.Provider({
                if (entity is ViewEntity) {
                    entity.parentConnection.player.viewManager
                } else {
                    ServerViewManagerImpl(entity.mcServer, entity.connection)
                }
            }))
        }
    }
}
