package de.johni0702.minecraft.view.impl.mixin;

import de.johni0702.minecraft.view.client.render.RenderPass;
import de.johni0702.minecraft.view.impl.client.render.ViewRenderManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.lib.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static de.johni0702.minecraft.betterportals.common.ExtensionsKt.approxEquals;

@SideOnly(Side.CLIENT)
@Mixin(RenderLivingBase.class)
public abstract class MixinRenderLivingBase<T extends EntityLivingBase> {

    private T entity;

    @Inject(method = "canRenderName", at = @At("HEAD"))
    private void rememberEntity(T entity, CallbackInfoReturnable<Boolean> cir) {
        this.entity = entity;
    }

    @Redirect(method = "canRenderName", at = @At(
            value = "FIELD",
            opcode = Opcodes.GETFIELD,
            target = "Lnet/minecraft/client/renderer/entity/RenderManager;renderViewEntity:Lnet/minecraft/entity/Entity;"
    ))
    private Entity getLogicalViewEntity(RenderManager renderManager) {
        RenderPass current = ViewRenderManager.Companion.getINSTANCE().getCurrent();
        if (current == null) {
            return renderManager.renderViewEntity;
        }
        Minecraft mc = Minecraft.getMinecraft();
        // If the entity in question is the player and also the logical view entity (which the third-person camera is
        // focused on), then we return the player as view entity instead of the camera entity (to suppress the name).
        // This does not account for the case where we're spectating another player in third person (as vanilla would)
        // because doing so is non-trivial and arguably one might want to see the name in that case anyway.
        if (entity == mc.player) {
            Vec3d interpEntityPos = entity.getPositionEyes(mc.getRenderPartialTicks());
            if (approxEquals(interpEntityPos, current.getCamera().getEyePosition(), 1e-4)) {
                return mc.player;
            }
        }
        return renderManager.renderViewEntity;
    }
}
