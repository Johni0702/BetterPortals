package de.johni0702.minecraft.betterportals.impl.nether.mixin;

import de.johni0702.minecraft.betterportals.impl.nether.common.BlockWithBPVersion;
import de.johni0702.minecraft.betterportals.impl.nether.common.blocks.BlockBetterNetherPortal;
import net.minecraft.block.Block;
import net.minecraft.block.BlockPortal;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

//#if MC>=11400
//$$ import net.minecraft.entity.player.PlayerEntity;
//$$ import net.minecraft.util.Direction;
//$$ import net.minecraft.util.math.shapes.ISelectionContext;
//$$ import net.minecraft.util.math.shapes.VoxelShape;
//$$ import net.minecraft.util.math.shapes.VoxelShapes;
//$$ import net.minecraft.world.IBlockReader;
//$$ import net.minecraft.world.IWorld;
//#else
import net.minecraft.block.material.Material;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.IBlockAccess;
//#endif

@Mixin(BlockPortal.class)
public abstract class MixinNetherPortalBlock extends Block implements BlockWithBPVersion {
    //#if MC>=11400
    //$$ public MixinNetherPortalBlock(Properties properties) { super(properties); }
    //#else
    public MixinNetherPortalBlock(Material materialIn) { super(materialIn); }
    //#endif

    private BlockBetterNetherPortal betterVersion;

    @Override
    public void enableBetterVersion(@NotNull Object mod) {
        betterVersion = new BlockBetterNetherPortal(mod);
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
    //$$ @Inject(method = "getOutlineShape", at = @At("HEAD"), cancellable = true)
    //#else
    //$$ @Inject(method = "getShape", at = @At("HEAD"), cancellable = true)
    //#endif
    //$$ private void getOutlineShape(BlockState state, IBlockReader world, BlockPos pos, ISelectionContext context, CallbackInfoReturnable<VoxelShape> ci) {
    //$$     if (betterVersion != null) {
    //$$         ci.setReturnValue(VoxelShapes.empty());
    //$$     }
    //$$ }
    //$$
    //$$ @Inject(method = "onEntityCollision", at = @At("HEAD"), cancellable = true)
    //$$ private void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity, CallbackInfo ci) {
    //$$     if (betterVersion != null) {
    //$$         if (entity instanceof PlayerEntity) {
    //$$             betterVersion.validatePortalOrDestroy(world, pos);
    //$$         }
    //$$         ci.cancel();
    //$$     }
    //$$ }
    //$$
    //#if FABRIC>=1
    //$$ @Inject(method = "getStateForNeighborUpdate", at = @At("HEAD"), cancellable = true)
    //#else
    //$$ @Inject(method = "updatePostPlacement", at = @At("HEAD"), cancellable = true)
    //#endif
    //$$ private void updatePostPlacement(BlockState state, Direction facing, BlockState facingState, IWorld world, BlockPos pos, BlockPos facingPos, CallbackInfoReturnable<BlockState> ci) {
    //$$     if (betterVersion != null) {
    //$$         if (world instanceof World) {
    //$$             betterVersion.validatePortalOrDestroy((World) world, pos);
    //$$         }
    //$$         ci.setReturnValue(state);
    //$$     }
    //$$ }
    //$$
    //$$ @Inject(method = "trySpawnPortal", at = @At("HEAD"), cancellable = true)
    //$$ private void trySpawnPortal(IWorld world, BlockPos pos, CallbackInfoReturnable<Boolean> ci) {
    //$$     if (betterVersion != null) {
    //$$         if (!(world instanceof World)) {
    //$$             ci.setReturnValue(false);
    //$$         } else {
    //$$             ci.setReturnValue(betterVersion.tryToLinkPortals((World) world, pos));
    //$$         }
    //$$     }
    //$$ }
    //#else
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

    @Inject(method = "neighborChanged", at = @At("HEAD"), cancellable = true)
    private void neighborChanged(IBlockState state, World world, BlockPos pos, Block block, BlockPos fromPos, CallbackInfo ci) {
        if (betterVersion != null) {
            betterVersion.neighborChanged(state, world, pos, block, fromPos);
            ci.cancel();
        }
    }

    @Inject(method = "trySpawnPortal", at = @At("HEAD"), cancellable = true)
    private void trySpawnPortal(World world, BlockPos pos, CallbackInfoReturnable<Boolean> ci) {
        if (betterVersion != null) {
            ci.setReturnValue(betterVersion.trySpawnPortal(world, pos));
        }
    }
    //#endif
}
