package com.wennest.placeable.mixin;

import com.wennest.placeable.Placeable;
import net.minecraft.block.BlockState;
import net.minecraft.block.SmallDripleafBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Relaxes the placement rule of vanilla {@link SmallDripleafBlock} so the
 * mod's floor predicate (any block with a top rim, leaves, or dirt path) is
 * accepted in addition to vanilla's clay / moss / dripleaf-stem-in-water
 * rule.
 *
 * <p>Defers to vanilla during worldgen and inside natural-tick frames so
 * vanilla feature placement and natural ecology spread are never widened.
 *
 * <p>{@code SmallDripleafBlock} fully overrides {@code canPlaceAt} and does
 * not super-call {@link net.minecraft.block.PlantBlock}, so
 * {@link PlantBlockMixin}'s transitive coverage does not apply.
 */
@Mixin(SmallDripleafBlock.class)
public class SmallDripleafBlockMixin {

    /**
     * HEAD-injected override of {@code SmallDripleafBlock.canPlaceAt}.
     * Standard relaxed-floor shape.
     */
    @Inject(method = "canPlaceAt", at = @At("HEAD"), cancellable = true)
    public void placeable$canPlantAnywhere(BlockState state, WorldView world, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (Placeable.shouldBypass(world, pos)) return;
        if (Placeable.isDisabled(state)) return;
        if (Placeable.isValidFloor(world, pos)) {
            cir.setReturnValue(true);
        }
    }
}
