package it.bisumto.placeable.mixin;

import it.bisumto.placeable.Placeable;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.MushroomPlantBlock;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.WorldView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MushroomPlantBlock.class)
public class MushroomPlantBlockMixin {
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

        if (!world.getBlockState(blockPos.add(0, -1, 0)).isIn(BlockTags.MUSHROOM_GROW_BLOCK)
                && world.getBaseLightLevel(blockPos, 0) >= 13) {
            ci.cancel();
            return;
        }

        if (random.nextInt(25) == 0) {
            int i = 5;
            for (BlockPos targetBlockPos : BlockPos.iterate(blockPos.add(-4, -1, -4), blockPos.add(4, 1, 4))) {
                if (world.getBlockState(targetBlockPos).isOf(Blocks.BROWN_MUSHROOM)
                        || world.getBlockState(targetBlockPos).isOf(Blocks.RED_MUSHROOM)) {
                    --i;
                    if (i <= 0) {
                        ci.cancel();
                        return;
                    }
                }
            }

            BlockPos randomBlockPos = blockPos.add(random.nextInt(3) - 1, random.nextInt(2) - random.nextInt(2), random.nextInt(3) - 1);
            for (int k = 0; k < 4; ++k) {
                BlockPos underBlockPos = randomBlockPos.add(0, -1, 0);
                BlockState underBlockState = world.getBlockState(underBlockPos);
                if (world.isAir(randomBlockPos)
                        && blockState.canPlaceAt(world, randomBlockPos)
                        && (underBlockState.isIn(BlockTags.MUSHROOM_GROW_BLOCK) || world.getBaseLightLevel(randomBlockPos, 0) < 13)
                ) {
                    blockPos = randomBlockPos;
                }

                randomBlockPos = blockPos.add(random.nextInt(3) - 1, random.nextInt(2) - random.nextInt(2), random.nextInt(3) - 1);
            }

            BlockPos underBlockPos = randomBlockPos.add(0, -1, 0);
            BlockState underBlockState = world.getBlockState(underBlockPos);
            if (world.isAir(randomBlockPos)
                    && blockState.canPlaceAt(world, randomBlockPos)
                    && (underBlockState.isIn(BlockTags.MUSHROOM_GROW_BLOCK) || world.getBaseLightLevel(randomBlockPos, 0) < 13)
            ) {
                world.setBlockState(randomBlockPos, blockState, 2);
            }
        }

        ci.cancel();
    }
}
