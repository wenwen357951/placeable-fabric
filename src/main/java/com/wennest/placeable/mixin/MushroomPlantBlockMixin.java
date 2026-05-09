package com.wennest.placeable.mixin;

import com.wennest.placeable.Placeable;
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

/**
 * Relaxes the placement rule of vanilla {@link MushroomPlantBlock} so the
 * mod's floor predicate (any block with a top rim, leaves, or dirt path) is
 * accepted in addition to vanilla's {@code MUSHROOM_GROW_BLOCK}-tagged
 * floors. The vanilla light-&lt;13 placement constraint is intentionally
 * dropped at placement time, making lit-mushroom decoration possible.
 *
 * <p>Defers to vanilla during worldgen and inside natural-tick frames so
 * vanilla feature placement and natural mushroom spread are never widened.
 *
 * <p>Also re-implements vanilla mushroom natural spread inside
 * {@code randomTick}. The re-implementation behaves as a moral
 * {@code @Overwrite} — it always cancels the original — but is safe because
 * the natural-tick flag set by {@link AbstractBlockStateNaturalTickMixin}
 * makes the inner {@code state.canPlaceAt} calls defer to vanilla floor
 * logic via {@link Placeable#shouldBypass}. Spread mechanics remain
 * bit-identical to vanilla; the relaxed {@code isValidFloor} cannot widen
 * them. Spread still gates on light &lt; 13.
 *
 * <p>{@code MushroomPlantBlock} fully overrides {@code canPlaceAt} and is
 * self-contained (no super-call to {@code PlantBlock}), so
 * {@link PlantBlockMixin} does not transitively cover this block.
 */
@Mixin(MushroomPlantBlock.class)
public class MushroomPlantBlockMixin {

    /**
     * Relaxes vanilla's mushroom floor rule for player placement. The
     * light-&lt;13 placement constraint is deliberately dropped here — that
     * is what makes lit-mushroom decoration possible — while the
     * {@code randomTick} hook below still gates SPREAD on light &lt; 13 to
     * keep natural ecology vanilla.
     */
    @Inject(method = "canPlaceAt", at = @At("HEAD"), cancellable = true)
    public void placeable$canPlantAnywhere(BlockState blockState, WorldView world, BlockPos blockPos, CallbackInfoReturnable<Boolean> cir) {
        // Defer to vanilla during worldgen and inside natural-tick frames.
        // Combined with AbstractBlockStateNaturalTickMixin, this defers any
        // canPlaceAt call originating from inside vanilla mushroom spread
        // back to vanilla logic — preserving the invariant that mushroom
        // spread targets must be opaque-full-cube + dim-light.
        if (Placeable.shouldBypass(world, blockPos)) {
            return;
        }

        // Per-plant config toggle.
        if (Placeable.isDisabled(blockState)) {
            return;
        }

        // Mod relaxation: any valid floor (top-rim, leaves, or dirt path)
        // satisfies placement. Vanilla's light-<13 check is intentionally
        // not replicated here — lit mushrooms are intended decoration, and
        // randomTick still enforces the spread light rule below.
        if (Placeable.isValidFloor(world, blockPos)) {
            cir.setReturnValue(true);
        }
    }

    /**
     * Replicates vanilla mushroom spread logic verbatim with the mod's
     * "no spread on inappropriate floors" gate. Structurally an
     * {@code @Overwrite} — always cancels the original — but safe because
     * the natural-tick flag makes the inner {@code state.canPlaceAt} calls
     * defer to vanilla floor logic, so spread cannot widen onto mod-relaxed
     * surfaces.
     */
    @Inject(method = "randomTick", at = @At("HEAD"), cancellable = true)
    public void placeable$randomTickMixin(BlockState blockState, ServerWorld world, BlockPos blockPos, Random random, CallbackInfo ci) {
        if (Placeable.isDisabled(blockState)) {
            return;
        }

        // First gate: if the floor immediately under the mushroom is NOT a
        // vanilla mushroom-grow block AND ambient light is bright (>=13),
        // there is no chance of vanilla spread happening anyway — bail
        // early, leaving the mushroom in place but inert.
        if (!world.getBlockState(blockPos.add(0, -1, 0)).isIn(BlockTags.MUSHROOM_GROW_BLOCK)
                && world.getBaseLightLevel(blockPos, 0) >= 13) {
            ci.cancel();
            return;
        }

        // Vanilla random-tick gate: only ~4% of ticks even attempt spread.
        if (random.nextInt(25) == 0) {
            // Vanilla overcrowding check: if 5+ mushrooms already exist in
            // the 9x3x9 box around this one, do not spread.
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

            // Vanilla random walk: select a candidate position, jiggle 4
            // times, then commit if vacant. Each step requires (a) the cell
            // is air, (b) state.canPlaceAt — guarded by the natural-tick
            // flag so it sees only vanilla rules, (c) the underneath block
            // is grow-tagged OR light < 13.
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

            // Final placement attempt at the random-walked position.
            BlockPos underBlockPos = randomBlockPos.add(0, -1, 0);
            BlockState underBlockState = world.getBlockState(underBlockPos);
            if (world.isAir(randomBlockPos)
                    && blockState.canPlaceAt(world, randomBlockPos)
                    && (underBlockState.isIn(BlockTags.MUSHROOM_GROW_BLOCK) || world.getBaseLightLevel(randomBlockPos, 0) < 13)
            ) {
                world.setBlockState(randomBlockPos, blockState, 2);
            }
        }

        // Always cancel the vanilla randomTick — this mixin has fully taken
        // over the spread logic. Without the cancel, vanilla would also
        // run, double-spreading.
        ci.cancel();
    }
}
