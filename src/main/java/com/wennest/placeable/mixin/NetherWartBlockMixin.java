package com.wennest.placeable.mixin;

import com.wennest.placeable.Placeable;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.NetherWartBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Targets {@link NetherWartBlock#randomTick}. Cancels growth unless the wart
 * is rooted on soul sand, preserving vanilla nether-wart ecology even when
 * the player has placed the wart on a relaxed floor.
 *
 * <p>{@code NetherWartBlock} owns its own {@code randomTick} growth logic;
 * relaxing only {@code canPlaceAt} via {@link PlantBlockMixin} would let
 * wart mature anywhere.
 */
@Mixin(NetherWartBlock.class)
public class NetherWartBlockMixin {
    /**
     * Cancels growth when the wart is not on soul sand, keeping vanilla
     * ecology untouched after player-relaxed placement.
     */
    @Inject(method = "randomTick", at = @At("HEAD"), cancellable = true)
    public void placeable$randomTickMixin(BlockState blockState, ServerWorld world, BlockPos blockPos, Random random, CallbackInfo ci) {
        if (Placeable.isDisabled(blockState)) {
            return;
        }

        BlockState underBlockState = world.getBlockState(blockPos.down());
        if (underBlockState.isOf(Blocks.SOUL_SAND)) {
            return;
        }

        ci.cancel();
    }
}
