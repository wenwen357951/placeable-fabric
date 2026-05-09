package com.wennest.placeable.mixin;

import com.wennest.placeable.Placeable;
import net.minecraft.block.BlockState;
import net.minecraft.block.PlantBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Relaxes the placement rule of vanilla {@link PlantBlock} so the mod's
 * floor predicate (any block with a top rim, leaves, or dirt path) is
 * accepted in addition to vanilla's specific requirements.
 *
 * <p>Defers to vanilla during worldgen and inside natural-tick frames so
 * vanilla feature placement and natural ecology spread are never widened.
 *
 * <p>{@code PlantBlock} is the shared superclass for the bulk of small
 * plants (grasses, ferns, flowers, saplings, dead bushes, fungi,
 * sea-pickles, ...). Most subclass overrides delegate via
 * {@code super.canPlaceAt(...)}, so this single HEAD injection transitively
 * covers them; the few subclasses that fully override {@code canPlaceAt}
 * (mushrooms, cactus, sugar cane, bamboo, big dripleaf, cocoa, ...) need
 * their own dedicated mixins in this same package.
 */
@Mixin(PlantBlock.class)
public class PlantBlockMixin {

    /**
     * HEAD-injected override of {@code PlantBlock.canPlaceAt}. Returns
     * {@code true} early when the relaxed floor rule applies; otherwise
     * defers to vanilla by NOT setting a return value.
     */
    @Inject(method = "canPlaceAt", at = @At("HEAD"), cancellable = true)
    public void placeable$canPlantAnywhere(BlockState state, WorldView world, BlockPos pos,
                                           CallbackInfoReturnable<Boolean> cir) {
        // Defer to vanilla during worldgen and inside natural-tick frames.
        // MUST be first — the relaxed floor rule below would otherwise widen
        // vanilla feature placement and natural ecology spread.
        if (Placeable.shouldBypass(world, pos)) return;
        // Per-plant config toggle (and global enable flag inside isDisabled).
        if (Placeable.isDisabled(state)) return;
        // Relaxed floor rule: top-rim block (or any non-empty block when
        // placedWithoutTopRim is true), leaves, or dirt path.
        if (Placeable.isValidFloor(world, pos)) {
            cir.setReturnValue(true);
        }
    }
}
