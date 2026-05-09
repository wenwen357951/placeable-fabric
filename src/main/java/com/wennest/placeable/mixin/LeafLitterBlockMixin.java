package com.wennest.placeable.mixin;

import com.wennest.placeable.Placeable;
import net.minecraft.block.BlockState;
//? if >=1.21.5
import net.minecraft.block.LeafLitterBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Relaxes the placement rule of vanilla {@code LeafLitterBlock} (added in
 * 1.21.5) so the mod's floor predicate (any block with a top rim, leaves,
 * or dirt path) is accepted in addition to vanilla's fully-solid-top-face
 * requirement.
 *
 * <p>Defers to vanilla during worldgen and inside natural-tick frames so
 * vanilla feature placement and natural ecology spread are never widened.
 *
 * <p>{@code LeafLitterBlock}'s vanilla {@code canPlaceAt} requires a
 * fully-solid top face (rejects slabs / stairs / leaves / dirt-path) and
 * does NOT super-call {@code PlantBlock.canPlaceAt}, so
 * {@link PlantBlockMixin}'s transitive coverage cannot reach this code path.
 *
 * <p><b>Stonecutter conditioning</b>: every line that names
 * {@code LeafLitterBlock} (the import) is gated with a single-line
 * {@code //? if >=1.21.5}; the {@code @Mixin}-annotated class body is
 * wrapped in a closed-scope block {@code //? if >=1.21.5 { ... //?}} so
 * Stonecutter comments out the entire mixin class on 1.21.1 / 1.21.4
 * builds. The mixins.json entry for this class is similarly gated via a
 * sibling line-conditional.
 */
//? if >=1.21.5 {
@Mixin(LeafLitterBlock.class)
public class LeafLitterBlockMixin {

    /**
     * HEAD-injected override of {@code LeafLitterBlock.canPlaceAt}. Standard
     * relaxed-floor shape.
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
//?}
