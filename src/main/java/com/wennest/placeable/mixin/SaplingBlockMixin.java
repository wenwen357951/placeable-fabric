package com.wennest.placeable.mixin;

import com.wennest.placeable.Placeable;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.SaplingBlock;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Targets {@link SaplingBlock#randomTick}. Cancels sapling growth (i.e.,
 * tree spawning) when the sapling is rooted on anything other than
 * dirt-tagged soil or farmland — preserving vanilla tree-ecology rules even
 * when the player has placed the sapling on a relaxed floor (e.g.,
 * cobblestone, stone slabs).
 *
 * <p>{@code SaplingBlock} owns its own {@code randomTick} growth pipeline;
 * relaxing {@code canPlaceAt} transitively via {@link PlantBlockMixin} is
 * not enough — without this guard a sapling on cobblestone would still grow
 * into a full tree.
 */
@Mixin(SaplingBlock.class)
public class SaplingBlockMixin {

    /**
     * Cancels growth when the sapling is not on dirt-tagged soil or
     * farmland; mirrors vanilla's implicit floor expectation.
     */
    @Inject(method = "randomTick", at = @At("HEAD"), cancellable = true)
    public void placeable$randomTickMixin(BlockState blockState, ServerWorld world, BlockPos blockPos, Random random, CallbackInfo ci) {
        if (Placeable.isDisabled(blockState)) {
            return;
        }

        BlockState floor = world.getBlockState(blockPos.down());
        if (!floor.isIn(BlockTags.DIRT) && !floor.isOf(Blocks.FARMLAND)) {
            ci.cancel();
        }
    }
}
