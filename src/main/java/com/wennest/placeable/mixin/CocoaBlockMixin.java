package com.wennest.placeable.mixin;

import com.wennest.placeable.Placeable;
import net.minecraft.block.BlockState;
import net.minecraft.block.CocoaBlock;
import net.minecraft.block.SideShapeType;
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

import static net.minecraft.block.HorizontalFacingBlock.FACING;

/**
 * Relaxes the placement rule of vanilla {@link CocoaBlock} so any block
 * exposing a rigid solid face on the FACING side accepts a cocoa pod, in
 * addition to vanilla's jungle-log-only rule.
 *
 * <p>Defers to vanilla during worldgen and inside natural-tick frames so
 * vanilla feature placement (e.g., jungle-tree generation that places cocoa
 * pods) and natural ecology spread are never widened.
 *
 * <p>Also prevents cocoa from advancing through its growth stages when
 * attached to anything other than a vanilla jungle log. This keeps mod-relaxed
 * placements decorative-only — cocoa pods stuck on a beacon will not ripen,
 * preserving vanilla farm balance.
 *
 * <p>Cocoa is unique among placeable plants: it attaches to the SIDE of a
 * block (jungle log) rather than sitting on a floor. The {@code isValidFloor}
 * predicate is therefore not consulted; the relaxation rule is "any block
 * whose face in the FACING direction is rigid-side-solid".
 */
@Mixin(CocoaBlock.class)
public class CocoaBlockMixin {

    /**
     * Relaxes vanilla cocoa attachment so any rigid-solid face works, not
     * just jungle logs. The bypass gate runs first so worldgen and
     * natural-tick frames see vanilla behavior unchanged.
     */
    @Inject(method = "canPlaceAt", at = @At("HEAD"), cancellable = true)
    public void placeable$canPlantAnywhere(BlockState blockState, WorldView world, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        // Defer to vanilla during worldgen and inside natural-tick frames.
        // MUST be the first statement: vanilla jungle-tree generation places
        // cocoa pods via TreeFeature, which eventually calls canPlaceAt on
        // a ChunkRegion world. Without this guard the relaxed rule could
        // override vanilla's strict jungle-log-only rule and let cocoa
        // generate on whatever block happened to sit beside the trunk.
        if (Placeable.shouldBypass(world, pos)) {
            return;
        }

        // Per-plant config toggle.
        if (Placeable.isDisabled(blockState)) {
            return;
        }

        // Cocoa-specific validation: the block in the FACING direction must
        // expose a rigid solid face. Any block satisfying this is treated
        // as a valid attachment surface (mod relaxation).
        BlockState faceBlockState = world.getBlockState(pos.offset(blockState.get(FACING)));
        if (faceBlockState.isSideSolid(world, pos, blockState.get(FACING), SideShapeType.RIGID)) {
            cir.setReturnValue(true);
        }
    }

    /**
     * Prevents cocoa from advancing through growth stages on non-jungle-log
     * surfaces. Keeps mod-relaxed cocoa placements decorative-only,
     * preserving vanilla farm balance.
     */
    @Inject(method = "randomTick", at = @At("HEAD"), cancellable = true)
    public void placeable$randomTickMixin(BlockState blockState, ServerWorld world, BlockPos blockPos, Random random, CallbackInfo ci) {
        if (Placeable.isDisabled(blockState)) {
            return;
        }

        // The block on the FACING side of the cocoa pod is its host. If it
        // is not a jungle log, cancel growth — the cocoa stays at its
        // placed age forever (until broken or the host changes).
        BlockState host = world.getBlockState(blockPos.offset(blockState.get(FACING)));
        if (!host.isIn(BlockTags.JUNGLE_LOGS)) {
            ci.cancel();
        }
    }
}
