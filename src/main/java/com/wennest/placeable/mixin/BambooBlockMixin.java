package com.wennest.placeable.mixin;

import com.wennest.placeable.Placeable;
import net.minecraft.block.BambooBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.WorldView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Relaxes the placement rule of vanilla {@link BambooBlock} so the mod's
 * floor predicate (any block with a top rim, leaves, or dirt path) is
 * accepted in addition to the {@link BlockTags#BAMBOO_PLANTABLE_ON} floors
 * vanilla allows.
 *
 * <p>Defers to vanilla during worldgen and inside natural-tick frames so
 * vanilla feature placement and natural ecology spread are never widened.
 *
 * <p>Also guards bamboo growth so a stalk placed on a non-bamboo-plantable
 * floor (e.g., cobblestone) does not silently propagate upward via random
 * tick, and substitutes a bamboo-shoot placement state when the player would
 * place regular bamboo on a mod-relaxed floor — mirroring vanilla's
 * shoot-first-then-stalk behavior on the wider floor set.
 */
@Mixin(BambooBlock.class)
public class BambooBlockMixin {

    /**
     * Centralized reference to the bamboo "shoot" / "sapling" block. Used by
     * both growth-prevention (via random-tick cancellation keyed on this
     * block's enable flag) and the placement-state substitution.
     *
     * <p>The vanilla field is still named {@code BAMBOO_SAPLING} on every
     * supported MC version (the shoot rename in 1.21.5 changed the
     * <em>class</em> {@code BambooSaplingBlock -> BambooShootBlock}, not the
     * registry / Blocks-class field).
     */
    @Unique
    private static final Block BAMBOO_KEY = Blocks.BAMBOO_SAPLING;

    /**
     * HEAD-injected override of {@code BambooBlock.canPlaceAt}. Standard
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

    /**
     * Cancels the bamboo random-tick growth path when the stack root rests on
     * a non-{@link BlockTags#BAMBOO_PLANTABLE_ON} floor. Without this guard a
     * player-placed bamboo on cobblestone would still grow upward into an
     * indefinitely tall stalk (vanilla never validates the root after the
     * initial placement).
     *
     * <p>The enable check keys on {@link #BAMBOO_KEY} (the shoot block), not
     * on {@code state}'s block, because the user-facing config toggle is
     * "BAMBOO" mapped to the shoot block in
     * {@link com.wennest.placeable.PlaceablePlants}.
     */
    @Inject(method = "randomTick", at = @At("HEAD"), cancellable = true)
    public void placeable$randomTickMixin(BlockState state, ServerWorld world, BlockPos pos,
                                          Random random, CallbackInfo ci) {
        if (Placeable.isDisabled(BAMBOO_KEY)) {
            return;
        }

        // Walk down to the root of the stack to find the actual floor.
        int i = 1;
        while (world.getBlockState(pos.down(i)).isOf(Blocks.BAMBOO)) {
            i++;
        }

        BlockState floor = world.getBlockState(pos.down(i));
        if (!floor.isIn(BlockTags.BAMBOO_PLANTABLE_ON)) {
            // Cancel vanilla's growth body — this stalk was placed on a
            // mod-relaxed floor and must NOT propagate upward.
            ci.cancel();
        }
    }

    /**
     * Substitutes a bamboo-shoot placement state when the player would place
     * regular bamboo on a mod-relaxed (non-bamboo-plantable) floor; mirrors
     * vanilla's shoot-first-then-stalk behavior on the wider floor set.
     *
     * <p>Injected at {@code RETURN} (not {@code TAIL}) with a null-check, so
     * this only substitutes when no prior return value is set; it never
     * stomps another mixin's earlier decision.
     */
    @Inject(method = "getPlacementState", at = @At("RETURN"), cancellable = true)
    public void placeable$getPlacementStateMixin(ItemPlacementContext ctx,
                                                 CallbackInfoReturnable<BlockState> cir) {
        // Do not stomp another mixin's / vanilla's existing placement.
        if (cir.getReturnValue() != null) return;
        // Defer to vanilla during worldgen and inside natural-tick frames.
        if (Placeable.shouldBypass(ctx.getWorld(), ctx.getBlockPos())) return;
        if (Placeable.isDisabled(BAMBOO_KEY)) return;

        if (Placeable.isValidFloor(ctx.getWorld(), ctx.getBlockPos())) {
            cir.setReturnValue(BAMBOO_KEY.getDefaultState());
        }
    }
}
