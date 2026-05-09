package com.wennest.placeable.mixin;

import com.wennest.placeable.Placeable;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CropBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Targets {@link CropBlock#randomTick} only. Cancels growth when the crop is
 * rooted on anything other than farmland, preserving vanilla farm-economy
 * balance even when the player has placed the crop on a relaxed floor.
 *
 * <h2>Why there is no {@code canPlaceAt} injection</h2>
 *
 * <p>Vanilla {@link CropBlock#canPlaceAt} reads (paraphrased):
 * <pre>
 *     return hasEnoughLightAt(world, pos) &amp;&amp; super.canPlaceAt(state, world, pos);
 * </pre>
 *
 * <p>The {@code hasEnoughLightAt} short-circuit (light level &ge; 9) is
 * evaluated BEFORE the {@code super.canPlaceAt} call. Even if the mod's
 * {@link PlantBlockMixin} ran on the super-call and returned {@code true},
 * the AND-chain would still gate placement on light, so a relaxed floor
 * would not actually let players place wheat in a dim corridor. Conversely,
 * any place where light &ge; 9 already satisfies vanilla farmland placement
 * rules — there is nothing useful to relax.
 *
 * <h2>Why the {@code randomTick} guard still matters</h2>
 *
 * <p>Although players cannot directly mod-place a crop on a non-farmland
 * block, a crop CAN end up on a relaxed floor if a future plant gets added
 * to {@link CropBlock}'s subclass tree, or via adjacent-block neighbour
 * interactions. This guard is the second line of defence: even if some
 * unusual code path lands a crop on a dirt-path or top-rim block, it will
 * not progress through growth stages.
 *
 * <p>Note: this mixin only RESTRICTS vanilla growth — it never widens
 * placement — so the bypass gate would be a no-op and is omitted.
 */
@Mixin(CropBlock.class)
public class CropBlockMixin {

    /**
     * Cancels random-tick growth advancement when the crop is not sitting
     * directly on farmland. Mod-relaxed placements via
     * {@link PlantBlockMixin}'s transitive coverage keep their visual state
     * but never mature.
     */
    @Inject(method = "randomTick", at = @At("HEAD"), cancellable = true)
    public void placeable$randomTickMixin(BlockState blockState, ServerWorld world, BlockPos blockPos, Random random, CallbackInfo ci) {
        if (Placeable.isDisabled(blockState)) {
            return;
        }

        // Crops only progress on farmland in vanilla — replicate that
        // restriction so non-farmland placements never mature into
        // harvestable produce.
        BlockState floor = world.getBlockState(blockPos.down());
        if (!floor.isOf(Blocks.FARMLAND)) {
            ci.cancel();
        }
    }
}
