package com.wennest.placeable.mixin;

import com.wennest.placeable.Placeable;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CactusBlock;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.EmptyBlockView;
import net.minecraft.world.WorldView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Relaxes the placement rule of vanilla {@link CactusBlock} so the mod's
 * floor predicate (any block with a top rim, leaves, or dirt path) is
 * accepted in addition to vanilla's "sand or another cactus" floor.
 *
 * <p>Defers to vanilla during worldgen and inside natural-tick frames so
 * vanilla feature placement and natural ecology spread are never widened.
 *
 * <p>Also prevents growth on non-sand floors so cacti placed on cobblestone
 * do not propagate into invalid stacks.
 *
 * <h2>What stays vanilla</h2>
 *
 * <p>Vanilla {@link CactusBlock#canPlaceAt} requires three conditions: (a)
 * all four horizontal neighbors must be non-solid, non-opaque, and not
 * lava-fluid; (b) the block below must be sand or another cactus; (c) the
 * block above must be free of fluid. The mod relaxes only condition (b).
 * Conditions (a) and (c) are PRESERVED because they encode genuine cactus
 * physics rather than floor restrictions: neighboring solid blocks would
 * crush the cactus on the next tick, and water above would destroy it.
 *
 * <p>The {@link Placeable#shouldBypass} guard runs FIRST, before the
 * cactus-on-cactus stack short-circuit, so the stack-validation path cannot
 * fire {@code setReturnValue(true)} during vanilla cactus-feature worldgen
 * and widen cactus placement checks. Player-action frames (runtime World, no
 * natural-tick depth) are the only context that reaches the relaxed rule.
 */
@Mixin(CactusBlock.class)
public class CactusBlockMixin {

    /**
     * Relaxes the floor condition of cactus placement while preserving the
     * 4-neighbor and fluid-above safety checks. The bypass gate runs first
     * so the cactus-stack short-circuit cannot bypass worldgen / natural-tick
     * deferral.
     */
    @Inject(method = "canPlaceAt", at = @At("HEAD"), cancellable = true)
    public void placeable$canPlantAnywhere(BlockState blockState, WorldView world, BlockPos blockPos, CallbackInfoReturnable<Boolean> cir) {
        // Defer to vanilla during worldgen and inside natural-tick frames.
        // MUST run before the cactus-on-cactus and isValidFloor branches —
        // those branches can call setReturnValue(true), which would widen
        // vanilla feature placement.
        if (Placeable.shouldBypass(world, blockPos)) {
            return;
        }

        // Per-plant config toggle.
        if (Placeable.isDisabled(blockState)) {
            return;
        }

        // Vanilla 4-direction safety check: a neighboring solid / opaque
        // block (or lava) would crush or destroy the cactus on the next
        // tick. Bail to the vanilla canPlaceAt, which will return false for
        // these positions. This is genuine cactus physics — the mod relaxes
        // WHERE cactus can stand, not whether it survives.
        for (Direction direction : Direction.Type.HORIZONTAL) {
            BlockPos neighbourPos = blockPos.offset(direction);
            BlockState neighbourState = world.getBlockState(neighbourPos);
            if (neighbourState.isSolidBlock(EmptyBlockView.INSTANCE, BlockPos.ORIGIN)
                    || neighbourState.isOpaque()
                    || world.getFluidState(neighbourPos).isIn(FluidTags.LAVA)) {
                return;
            }
        }

        // Stack-on-cactus (vanilla path) OR mod-relaxed valid floor, AND
        // the block above must be fluid-free (water would destroy cactus).
        BlockState underBlockState = world.getBlockState(blockPos.down());
        if ((underBlockState.isOf(Blocks.CACTUS) || Placeable.isValidFloor(world, blockPos))
                && world.getFluidState(blockPos.up()).isEmpty()) {
            cir.setReturnValue(true);
        }
    }

    /**
     * Prevents cactus on non-sand floors from growing taller. Walks down the
     * cactus column (max 2 segments below — vanilla's sand-search window)
     * and aborts the random tick if the foundation isn't sand.
     */
    @Inject(method = "randomTick", at = @At("HEAD"), cancellable = true)
    public void placeable$randomTickMixin(BlockState blockState, ServerWorld world, BlockPos blockPos, Random random, CallbackInfo ci) {
        if (Placeable.isDisabled(blockState)) {
            return;
        }

        // Walk down through any contiguous cactus blocks to find the actual
        // ground beneath the column. Vanilla allows cactus stacks up to 3
        // tall, so look at most 2 blocks down before settling on the ground
        // search position.
        int i = 1;
        while (i < 3 && world.getBlockState(blockPos.down(i)).isOf(Blocks.CACTUS)) {
            ++i;
        }

        BlockPos groundBlockPos = blockPos.down(i);
        BlockState groundBlockState = world.getBlockState(groundBlockPos);
        if (!groundBlockState.isIn(BlockTags.SAND)) {
            // Cancel growth — but leave the existing cactus standing. This
            // is what makes mod-placed cactus on cobblestone "decorative":
            // it persists but never grows into an invalid stacked state.
            ci.cancel();
        }
    }
}
