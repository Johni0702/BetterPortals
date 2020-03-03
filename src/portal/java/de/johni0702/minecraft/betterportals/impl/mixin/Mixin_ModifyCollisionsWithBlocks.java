//#if MC>=11400
//$$ package de.johni0702.minecraft.betterportals.impl.mixin;
//$$
//$$ import de.johni0702.minecraft.betterportals.impl.common.PortalManagerImpl;
//$$ import net.minecraft.entity.Entity;
//$$ import net.minecraft.util.math.AxisAlignedBB;
//$$ import net.minecraft.util.math.shapes.VoxelShape;
//$$ import net.minecraft.world.IWorld;
//$$ import net.minecraft.world.World;
//$$ import org.spongepowered.asm.mixin.Mixin;
//$$
//$$ import javax.annotation.Nullable;
//$$ import java.util.stream.Stream;
//$$
//$$ @Mixin(World.class)
//$$ public abstract class Mixin_ModifyCollisionsWithBlocks implements IWorld {
//$$     @Override
//$$     public Stream<VoxelShape> getCollisionShapes(@Nullable Entity entity, AxisAlignedBB box) {
//$$         Stream<VoxelShape> result = IWorld.super.getCollisionShapes(entity, box);
//$$         result = PortalManagerImpl.EventHandler.INSTANCE.modifyCollisionsWithBlocks(entity, box, result);
//$$         return result;
//$$     }
//$$ }
//#endif
