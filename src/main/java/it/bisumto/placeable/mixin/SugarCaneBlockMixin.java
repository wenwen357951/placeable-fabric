package it.bisumto.placeable.mixin;

import it.bisumto.placeable.Placeable;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.SugarCaneBlock;
import net.minecraft.fluid.FluidState;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.WorldView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(SugarCaneBlock.class)
public class SugarCaneBlockMixin {

    // PLACEABLE
    @Inject(method = "canPlaceAt", at = @At("HEAD"), cancellable = true)
    public void canPlantAnywhere(BlockState blockState, WorldView world, BlockPos blockPos, CallbackInfoReturnable<Boolean> cir) {
        if (Placeable.isDisable(blockState)) {
            return;
        }

        if (Placeable.isValidFloor(world, blockPos)) {
            cir.setReturnValue(true);
        }
    }

    // PREVENT GROWING
    @Inject(method = "randomTick", at = @At("HEAD"), cancellable = true)
    public void randomTickMixin(BlockState blockState, ServerWorld world, BlockPos blockPos, Random random, CallbackInfo ci) {
        if (Placeable.isDisable(blockState)) {
            return;
        }

        int i = 1;
        while (i < 3 && world.getBlockState(blockPos.down(i)).isOf(Blocks.SUGAR_CANE)) {
            ++i;
        }

        BlockPos groundBlockPos = blockPos.down(i);
        BlockState groundBlockState = world.getBlockState(groundBlockPos);
        if (!groundBlockState.isIn(BlockTags.DIRT) && !groundBlockState.isIn(BlockTags.SAND)) {
            ci.cancel();
            return;
        }

        for (Direction direction : Direction.Type.HORIZONTAL) {
            BlockState targetBlockState = world.getBlockState(groundBlockPos.offset(direction));
            FluidState targetFluidState = world.getFluidState(groundBlockPos.offset(direction));

            if (targetFluidState.isIn(FluidTags.WATER) || targetBlockState.isOf(Blocks.FROSTED_ICE)) {
                return;
            }
        }

        ci.cancel();
    }
}
