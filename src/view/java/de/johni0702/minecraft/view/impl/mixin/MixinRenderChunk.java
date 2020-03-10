//#if MC>=11400
//$$ // fixed \o/
//#else
package de.johni0702.minecraft.view.impl.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.entity.Entity;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.lib.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(RenderChunk.class)
@SideOnly(Side.CLIENT)
public abstract class MixinRenderChunk {
    // MC uses the player instead of the view entity for no apparent reason and that is incorrect once we have portals
    @Redirect(method = "getDistanceSq", at = @At(value = "FIELD", opcode = Opcodes.GETFIELD, target = "Lnet/minecraft/client/Minecraft;player:Lnet/minecraft/client/entity/EntityPlayerSP;"))
    private EntityPlayerSP getViewEntity(Minecraft mc) {
        Entity viewEntity = mc.getRenderViewEntity();
        return viewEntity instanceof EntityPlayerSP ? (EntityPlayerSP) viewEntity : mc.player;
    }
}
//#endif
