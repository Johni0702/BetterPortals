package de.johni0702.minecraft.view.impl.mixin;

import de.johni0702.minecraft.view.client.render.ChunkVisibilityDetail;
import de.johni0702.minecraft.view.client.render.RenderPass;
import de.johni0702.minecraft.view.impl.client.render.ViewRenderManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(RenderGlobal.class)
@SideOnly(Side.CLIENT)
public abstract class MixinRenderGlobal_VC {
    // Vivecraft adds its own flood fill origin patch, so we need to target the second BlockPos as well. Otherwise
    // identical to MixinRenderGlobal (we could remove the one there when VC is installed but it doesn't hurt either).
    @Redirect(method = "setupTerrain", at = @At(value = "NEW", target = "net/minecraft/util/math/BlockPos", ordinal = 1))
    private BlockPos getChunkVisibilityFloodFillOrigin(double orgX, double orgY, double orgZ) {
        RenderPass current = ViewRenderManager.Companion.getINSTANCE().getCurrent();
        if (current != null) {
            BlockPos origin = current.get(ChunkVisibilityDetail.class).getOrigin();
            if (origin != null) {
                return origin;
            }
        }
        return new BlockPos(orgX, orgY, orgZ);
    }
}
