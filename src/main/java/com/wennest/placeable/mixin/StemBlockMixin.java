package com.wennest.placeable.mixin;

import com.wennest.placeable.Placeable;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.StemBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Targets {@link StemBlock#randomTick} (melon and pumpkin stems). Cancels
 * growth when the stem is rooted on anything other than farmland —
 * preventing player-placed stems on relaxed floors from advancing through
 * their growth stages and spawning fruit on invalid soils.
 *
 * <p>{@code StemBlock} extends {@code PlantBlock} but defines its own
 * {@code randomTick} growth pipeline; only intercepting {@code canPlaceAt}
 * via {@link PlantBlockMixin} would still let stems mature on cobblestone.
 */
@Mixin(StemBlock.class)
public class StemBlockMixin {
    /**
     * Cancels growth when the stem is not on farmland, keeping vanilla
     * fruit-spawning ecology intact even after player-relaxed placement.
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
