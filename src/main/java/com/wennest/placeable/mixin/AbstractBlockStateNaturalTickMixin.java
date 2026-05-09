package com.wennest.placeable.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.wennest.placeable.Placeable;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Brackets every vanilla natural-tick frame so {@link Placeable#isNaturalTick()}
 * returns {@code true} while {@code randomTick} / {@code scheduledTick} is
 * executing. This is the sole mechanism by which the mod's {@code canPlaceAt}
 * mixins detect "vanilla natural-evolution" context.
 *
 * <h2>Vanilla dispatch chain</h2>
 *
 * <p>Both target methods on {@code AbstractBlock$AbstractBlockState} are pure
 * dispatch:
 * <pre>
 *   invoke virtual getBlock()
 *   invoke virtual asBlockState()
 *   invoke virtual Block.randomTick(BlockState, ServerWorld, BlockPos, Random)
 * </pre>
 * {@code BlockState} extends {@code AbstractBlock$AbstractBlockState} and does
 * <em>not</em> override these methods. The only callers in vanilla are
 * {@code ServerWorld.tickChunk} (random) and {@code ServerWorld.tickBlock}
 * (scheduled). Targeting the inner class is therefore necessary AND
 * sufficient to catch every natural-tick path that goes through the
 * BlockState layer.
 *
 * <h2>Why bracket natural ticks</h2>
 *
 * <p>Natural evolution (mushroom spread, cactus stack validation, sugar-cane
 * growth, etc.) MUST proceed exactly as in vanilla. Vanilla mushroom
 * {@code randomTick} for example calls {@code BlockState.canPlaceAt}
 * internally; without this bracket the mod's {@code MushroomPlantBlockMixin}
 * would widen the spread surface to slabs, stairs, leaves, and dirt-paths.
 *
 * <p>The natural-tick depth is the ThreadLocal half of the "is this a player
 * action?" question. The other half ({@code world instanceof World}) is
 * checked by {@link Placeable#shouldBypass}.
 *
 * <h2>Why {@code @WrapMethod} and not {@code @Inject}</h2>
 *
 * <p>{@code @Inject(at = @At("RETURN"))} does NOT fire on exception paths.
 * If vanilla {@code randomTick} throws (concurrent chunk modification, etc.),
 * a HEAD-then-RETURN injection would leak {@code NATURAL_TICK_DEPTH} — every
 * subsequent player placement on that thread would be wrongly classified as
 * "natural tick" and refuse mod intervention. {@code @WrapMethod} from
 * MixinExtras gives proper {@code try/finally} semantics, so the depth
 * counter is always restored. Loom and Fabric API ship MixinExtras at
 * runtime; no extra dependency declaration is needed.
 */
@Mixin(targets = "net.minecraft.block.AbstractBlock$AbstractBlockState")
public abstract class AbstractBlockStateNaturalTickMixin {

    /**
     * Wraps {@code AbstractBlock$AbstractBlockState.randomTick(ServerWorld,
     * BlockPos, Random)} — the random-tick dispatch entry point.
     *
     * <p>Increments {@link Placeable#enterNaturalTick()} before calling
     * vanilla, decrements via {@code finally} so an exceptional exit still
     * restores the counter.
     */
    @WrapMethod(method = "randomTick")
    private void placeable$wrapRandomTick(
            ServerWorld world, BlockPos pos, Random random,
            Operation<Void> original) {
        Placeable.enterNaturalTick();
        try {
            original.call(world, pos, random);
        } finally {
            // try/finally is mandatory — see "Why @WrapMethod" in the class
            // Javadoc. A leaked depth here would silently disable player
            // placement on the affected thread until JVM restart.
            Placeable.exitNaturalTick();
        }
    }

    /**
     * Wraps {@code AbstractBlock$AbstractBlockState.scheduledTick(ServerWorld,
     * BlockPos, Random)} — the scheduled-tick dispatch entry point.
     *
     * <p>Symmetric to {@link #placeable$wrapRandomTick}. Scheduled ticks
     * drive e.g., big-dripleaf upgrowth, sugar-cane scheduled-grow paths,
     * and any future scheduled-tick plant logic.
     */
    @WrapMethod(method = "scheduledTick")
    private void placeable$wrapScheduledTick(
            ServerWorld world, BlockPos pos, Random random,
            Operation<Void> original) {
        Placeable.enterNaturalTick();
        try {
            original.call(world, pos, random);
        } finally {
            Placeable.exitNaturalTick();
        }
    }
}
