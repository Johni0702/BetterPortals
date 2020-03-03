package de.johni0702.minecraft.betterportals.impl.vanilla.mixin;

import de.johni0702.minecraft.betterportals.impl.vanilla.common.BlockWithBPVersion;
import de.johni0702.minecraft.betterportals.impl.vanilla.common.blocks.BlockBetterEndPortal;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.BlockEndPortal;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

//#if MC>=11400
//$$ import de.johni0702.minecraft.betterportals.impl.vanilla.common.blocks.TileEntityBetterEndPortal;
//$$ import de.johni0702.minecraft.betterportals.impl.vanilla.common.entity.EndPortalEntity;
//$$ import net.minecraft.block.Blocks;
//$$ import net.minecraft.block.EndPortalFrameBlock;
//$$ import net.minecraft.entity.player.PlayerEntity;
//$$ import net.minecraft.util.Direction;
//$$ import net.minecraft.util.math.shapes.ISelectionContext;
//$$ import net.minecraft.util.math.shapes.VoxelShape;
//$$ import net.minecraft.util.math.shapes.VoxelShapes;
//$$ import net.minecraft.world.IBlockReader;
//$$ import net.minecraft.world.IWorld;
//#else
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.world.IBlockAccess;
//#endif

@Mixin(BlockEndPortal.class)
public abstract class MixinEndPortalBlock extends BlockContainer implements BlockWithBPVersion {
    //#if MC>=11400
    //$$ protected MixinEndPortalBlock(Properties builder) { super(builder); }
    //#else
    public MixinEndPortalBlock(Material materialIn) { super(materialIn); }
    //#endif

    private BlockBetterEndPortal betterVersion;

    @Override
    public void enableBetterVersion(@NotNull Object mod) {
        betterVersion = new BlockBetterEndPortal();
    }

    @Override
    public EnumBlockRenderType getRenderType(IBlockState state) {
        if (betterVersion != null) {
            return EnumBlockRenderType.INVISIBLE;
        }
        return super.getRenderType(state);
    }

    //#if MC>=11400
    //#if FABRIC>=1
    //$$ @Inject(method = "createBlockEntity", at = @At("HEAD"), cancellable = true)
    //#else
    //$$ @Inject(method = "createNewTileEntity", at = @At("HEAD"), cancellable = true)
    //#endif
    //$$ private void createNewTileEntity(IBlockReader world, CallbackInfoReturnable<TileEntity> ci) {
    //$$     if (betterVersion != null) {
    //$$         ci.setReturnValue(new TileEntityBetterEndPortal());
    //$$     }
    //$$ }
    //$$
    //#if FABRIC>=1
    //$$ @Inject(method = "getOutlineShape", at = @At("HEAD"), cancellable = true)
    //#else
    //$$ @Inject(method = "getShape", at = @At("HEAD"), cancellable = true)
    //#endif
    //$$ private void getShape(BlockState state, IBlockReader world, BlockPos pos, ISelectionContext context, CallbackInfoReturnable<VoxelShape> ci) {
    //$$     if (betterVersion != null) {
    //$$         ci.setReturnValue(VoxelShapes.empty());
    //$$     }
    //$$ }
    //$$
    //$$ @Inject(method = "onEntityCollision", at = @At("HEAD"), cancellable = true)
    //$$ private void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity, CallbackInfo ci) {
    //$$     if (betterVersion != null) {
    //$$         if (entity instanceof PlayerEntity) {
    //$$             betterVersion.makePortal(world, pos);
    //$$         }
    //$$         ci.cancel();
    //$$     }
    //$$ }
    //$$
    //$$ @Override
    //$$ public void onBlockAdded(BlockState blockState, World world, BlockPos blockPos, BlockState oldState, boolean isMoving) {
    //$$     super.onBlockAdded(blockState, world, blockPos, oldState, isMoving);
    //$$     if (betterVersion != null) {
    //$$         betterVersion.makePortal(world, blockPos);
    //$$     }
    //$$ }
    //$$
    //$$ @Override
    //$$ public BlockState updatePostPlacement(BlockState stateIn, Direction facing, BlockState facingState, IWorld world, BlockPos pos, BlockPos facingPos) {
    //$$     if (betterVersion != null) {
    //$$         if (EndPortalFrameBlock.getOrCreatePortalShape().match(world, pos) == null
    //$$                 && BlockBetterEndPortal.Companion.getExitPattern().match(world, pos) == null) {
    //$$             for (EndPortalEntity entity : world.getEntitiesWithinAABB(EndPortalEntity.class, new AxisAlignedBB(pos))) {
    //$$                 entity.remove();
    //$$             }
    //$$             return Blocks.AIR.getDefaultState();
    //$$         } else {
    //$$             return stateIn;
    //$$         }
    //$$     }
    //$$     return super.updatePostPlacement(stateIn, facing, facingState, world, pos, facingPos);
    //$$ }
    //#else
    @Inject(method = "createNewTileEntity", at = @At("HEAD"), cancellable = true)
    private void createNewTileEntity(World world, int meta, CallbackInfoReturnable<TileEntity> ci) {
        if (betterVersion != null) {
            ci.setReturnValue(betterVersion.createNewTileEntity(world, meta));
        }
    }

    @Inject(method = "getBoundingBox", at = @At("HEAD"), cancellable = true)
    private void getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos, CallbackInfoReturnable<AxisAlignedBB> ci) {
        if (betterVersion != null) {
            ci.setReturnValue(betterVersion.getBoundingBox(state, source, pos));
        }
    }

    @Inject(method = "onEntityCollidedWithBlock", at = @At("HEAD"), cancellable = true)
    private void onEntityCollidedWithBlock(World world, BlockPos pos, IBlockState state, Entity entity, CallbackInfo ci) {
        if (betterVersion != null) {
            betterVersion.onEntityCollidedWithBlock(world, pos, state, entity);
            ci.cancel();
        }
    }

    @Override
    public void onBlockAdded(World world, BlockPos pos, IBlockState state) {
        if (betterVersion != null) {
            betterVersion.onBlockAdded(world, pos, state);
        } else {
            super.onBlockAdded(world, pos, state);
        }
    }

    @Override
    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block block, BlockPos fromPos) {
        if (betterVersion != null) {
            betterVersion.neighborChanged(state, world, pos, block, fromPos);
        } else {
            super.neighborChanged(state, world, pos, block, fromPos);
        }
    }
    //#endif
}
