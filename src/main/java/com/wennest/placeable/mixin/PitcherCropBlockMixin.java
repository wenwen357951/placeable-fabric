package com.wennest.placeable.mixin;

import com.wennest.placeable.Placeable;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.PitcherCropBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Targets {@link PitcherCropBlock#randomTick}. Cancels growth unless the
 * pitcher crop is rooted on farmland, keeping the vanilla
 * farming-on-farmland-only ecology rule intact even when the player places
 * the crop on a relaxed floor.
 *
 * <p>{@code PitcherCropBlock} chains through {@code TallPlantBlock.canPlaceAt}
 * (so {@link PlantBlockMixin} covers placement transitively) but provides
 * its own {@code randomTick} growth pipeline that needs an explicit floor
 * guard here.
 */
@Mixin(PitcherCropBlock.class)
public class PitcherCropBlockMixin {
    /**
     * Cancels growth when the pitcher crop is not on farmland, leaving
     * vanilla growth ecology untouched after player-relaxed placement.
     */
    @Inject(method = "randomTick", at = @At("HEAD"), cancellable = true)
    public void placeable$randomTickMixin(BlockState blockState, ServerWorld world, BlockPos blockPos, Random random, CallbackInfo ci) {
        if (Placeable.isDisabled(blockState)) {
            return;
        }

        BlockState underBlockState = world.getBlockState(blockPos.down());
        if (underBlockState.isOf(Blocks.FARMLAND)) {
            return;
        }

        ci.cancel();
    }
}
