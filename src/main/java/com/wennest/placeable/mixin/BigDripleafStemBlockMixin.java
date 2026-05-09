package com.wennest.placeable.mixin;

import com.wennest.placeable.Placeable;
import net.minecraft.block.BigDripleafStemBlock;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Relaxes the placement rule of vanilla {@link BigDripleafStemBlock} — the
 * stem segment that grows beneath a big dripleaf head — so the mod's floor
 * predicate (any block with a top rim, leaves, or dirt path) is accepted in
 * addition to vanilla's stem-specific requirements.
 *
 * <p>Defers to vanilla during worldgen and inside natural-tick frames so
 * vanilla feature placement and natural ecology spread are never widened.
 *
 * <p>{@code BigDripleafStemBlock} fully overrides {@code canPlaceAt} (it
 * validates that there is a dripleaf head above and a compatible block
 * below), so {@link PlantBlockMixin}'s transitive coverage does not apply.
 */
@Mixin(BigDripleafStemBlock.class)
public class BigDripleafStemBlockMixin {

    /**
     * HEAD-injected override of {@code BigDripleafStemBlock.canPlaceAt}.
     * Standard relaxed-floor shape.
     */
    @Inject(method = "canPlaceAt", at = @At("HEAD"), cancellable = true)
    public void placeable$canPlantAnywhere(BlockState state, WorldView world, BlockPos pos,
                                           CallbackInfoReturnable<Boolean> cir) {
        // Defer to vanilla during worldgen and inside natural-tick frames.
        if (Placeable.shouldBypass(world, pos)) return;
        if (Placeable.isDisabled(state)) return;
        if (Placeable.isValidFloor(world, pos)) {
            cir.setReturnValue(true);
        }
    }
}
