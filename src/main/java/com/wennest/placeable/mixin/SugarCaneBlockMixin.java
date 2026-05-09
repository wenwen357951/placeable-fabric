package com.wennest.placeable.mixin;

import com.wennest.placeable.Placeable;
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

/**
 * Relaxes the placement rule of vanilla {@link SugarCaneBlock} so the mod's
 * floor predicate (any block with a top rim, leaves, or dirt path) is
 * accepted in addition to vanilla's "dirt or sand adjacent to water" rule.
 *
 * <p>Defers to vanilla during worldgen and inside natural-tick frames so
 * vanilla feature placement and natural ecology spread are never widened.
 *
 * <p>Also prevents random-tick growth when the cane stack is rooted on a
 * non-vanilla floor (anything other than dirt/sand with adjacent water or
 * frosted ice). This keeps mod-placed cane decorative — it persists but
 * never propagates upward into a stack that would violate vanilla growth
 * invariants.
 *
 * <p>{@code SugarCaneBlock} fully overrides {@code canPlaceAt} (no
 * super-call), so {@link PlantBlockMixin}'s transitive coverage does not
 * apply.
 */
@Mixin(SugarCaneBlock.class)
public class SugarCaneBlockMixin {

    /**
     * HEAD-injected override of {@code SugarCaneBlock.canPlaceAt}. Standard
     * relaxed-floor shape.
     */
    @Inject(method = "canPlaceAt", at = @At("HEAD"), cancellable = true)
    public void placeable$canPlantAnywhere(BlockState state, WorldView world, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (Placeable.shouldBypass(world, pos)) return;
        if (Placeable.isDisabled(state)) return;
        if (Placeable.isValidFloor(world, pos)) {
            cir.setReturnValue(true);
        }
    }

    /**
     * Cancels growth when the cane stack is rooted on a non-vanilla floor.
     * Walks down the stack to find the actual ground, then checks for
     * dirt/sand + adjacent water or frosted ice (vanilla growth rule).
     */
    @Inject(method = "randomTick", at = @At("HEAD"), cancellable = true)
    public void randomTickMixin(BlockState blockState, ServerWorld world, BlockPos blockPos, Random random, CallbackInfo ci) {
        if (Placeable.isDisabled(blockState)) {
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
