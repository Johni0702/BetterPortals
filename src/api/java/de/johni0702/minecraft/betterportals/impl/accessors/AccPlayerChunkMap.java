//#if MC<11400
package de.johni0702.minecraft.betterportals.impl.accessors;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.management.PlayerChunkMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(PlayerChunkMap.class)
public interface AccPlayerChunkMap {
    @Accessor
    List<EntityPlayerMP> getPlayers();
}
//#endif
