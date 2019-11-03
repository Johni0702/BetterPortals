//#if MC>=11400
//$$ package de.johni0702.minecraft.view.impl.mixin;
//$$
//$$ import de.johni0702.minecraft.view.impl.client.render.IActiveRenderInfo;
//$$ import net.minecraft.client.renderer.ActiveRenderInfo;
//$$ import net.minecraft.entity.Entity;
//$$ import net.minecraft.util.math.Vec3d;
//$$ import net.minecraft.world.IBlockReader;
//$$ import org.jetbrains.annotations.NotNull;
//$$ import org.spongepowered.asm.mixin.Mixin;
//$$ import org.spongepowered.asm.mixin.Shadow;
//$$
//$$ @Mixin(ActiveRenderInfo.class)
//$$ public abstract class Mixin_ActiveRenderInfo_From_Camera implements IActiveRenderInfo {
//$$     @Shadow public IBlockReader world;
//$$     @Shadow public Entity renderViewEntity;
//$$
//$$     @Shadow protected abstract void setPostion(Vec3d pos);
//$$     @Shadow protected abstract void setDirection(float yaw, float pitch);
//$$
//$$     @Override
//$$     public void update(@NotNull Entity entity, @NotNull de.johni0702.minecraft.view.client.render.Camera camera) {
//$$         this.world = entity.world;
//$$         this.renderViewEntity = entity;
//$$         this.setPostion(camera.getViewPosition());
//$$         this.setDirection((float) camera.getViewRotation().y, (float) camera.getViewRotation().x);
//$$     }
//$$ }
//#endif
