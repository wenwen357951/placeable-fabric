package com.wennest.placeable.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.wennest.placeable.Placeable;
import net.minecraft.item.BoneMealItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Brackets bone-meal use so vanilla bone-meal output stays bit-identical to
 * vanilla on a mod-relaxed world.
 *
 * <h2>Why this mixin exists</h2>
 *
 * <p>Vanilla {@code BoneMealItem.useOnGround(ItemStack, World, BlockPos, Direction)}
 * and {@code BoneMealItem.useOnFertilizable(ItemStack, World, BlockPos)} call
 * {@link net.minecraft.block.BlockState#canPlaceAt} <em>directly</em> while
 * scanning the spawn surfaces around the bone-mealed position. The
 * {@code useOnFertilizable} entry point in turn invokes
 * {@link net.minecraft.block.Fertilizable#grow}, and several Fertilizable
 * blocks ({@code BushBlock}, {@code FireflyBushBlock}) reach
 * {@code BlockState.canPlaceAt} via {@code Fertilizable.findPosToSpreadTo}.
 *
 * <p>Both code paths run on a {@code ServerWorld} (a {@link World} subtype),
 * so the world-type half of {@link Placeable#shouldBypass} does NOT fire.
 * They also run outside any {@code AbstractBlock$AbstractBlockState.randomTick}
 * or {@code scheduledTick} frame, so the natural-tick ThreadLocal half does
 * NOT fire either. Without this dedicated bracket, the mod's relaxed
 * {@code canPlaceAt} mixins widen the spawn-surface decision when bone meal
 * fans out flowers / tall grass / bushes — which would change vanilla
 * output.
 *
 * <h2>Policy</h2>
 *
 * <p>Although the player triggers the bone-meal use, the actual placement
 * decisions are vanilla's natural-spread code path. Bone-meal output is
 * therefore deferred to vanilla rather than treated as player intent.
 *
 * <p>The implementation simply increments
 * {@link Placeable#enterNaturalTick()} for the duration of each call. Every
 * {@code canPlaceAt} mixin already consults {@link Placeable#shouldBypass},
 * which checks {@link Placeable#isNaturalTick()}; once the depth is
 * positive, the mod's relaxed rules step aside and vanilla's strict rules
 * decide every spawn site.
 *
 * <h2>Why {@code @WrapMethod} and not {@code @Inject}</h2>
 *
 * <p>Same reasoning as {@link AbstractBlockStateNaturalTickMixin}: a plain
 * {@code @Inject(at = @At("RETURN"))} would NOT fire on exception paths,
 * leaking {@code NATURAL_TICK_DEPTH} on the calling thread.
 * {@code @WrapMethod} gives proper {@code try/finally} semantics so the
 * depth is always restored.
 *
 * <h2>Server-side mixin</h2>
 *
 * <p>Bone meal is server-authoritative — the client predicts the use, but
 * the actual block placement happens on the server. This mixin therefore
 * lives in the {@code "mixins"} array of {@code placeable.mixins.json}, not
 * the {@code "client"} array.
 */
@Mixin(BoneMealItem.class)
public abstract class BoneMealItemMixin {

    /**
     * Wraps {@code BoneMealItem.useOnGround(ItemStack, World, BlockPos,
     * Direction)} — vanilla's "scan adjacent grass blocks and spawn flowers /
     * tall grass / bushes" entry point. While this method runs, every nested
     * {@code BlockState.canPlaceAt} call hits the natural-tick branch of
     * {@link Placeable#shouldBypass} and falls through to vanilla's strict
     * placement rule, producing vanilla-identical bone-meal output.
     */
    @WrapMethod(method = "useOnGround")
    private static boolean placeable$wrapUseOnGround(
            ItemStack stack, World world, BlockPos pos, Direction side,
            Operation<Boolean> original) {
        Placeable.enterNaturalTick();
        try {
            return original.call(stack, world, pos, side);
        } finally {
            // try/finally is mandatory — without it, an exception escaping
            // vanilla's spawn-loop would leak NATURAL_TICK_DEPTH on this
            // thread and silently disable mod intervention for every later
            // player placement until JVM restart.
            Placeable.exitNaturalTick();
        }
    }

    /**
     * Wraps {@code BoneMealItem.useOnFertilizable(ItemStack, World, BlockPos)}
     * — vanilla's "block implements {@link net.minecraft.block.Fertilizable};
     * call its {@code grow} method" entry point.
     *
     * <p>Two 1.21.5+ Fertilizable blocks — {@code BushBlock} (entry
     * {@code BUSH}) and {@code FireflyBushBlock} (entry {@code FIREFLY_BUSH})
     * — reach {@code BlockState.canPlaceAt} from inside {@code grow} via
     * {@code Fertilizable.findPosToSpreadTo}. Without this bracket,
     * bone-mealing a bush adjacent to a slab would let the bush spread onto
     * the slab top — vanilla never does that.
     *
     * <p>Any future Fertilizable plant whose {@code grow} touches
     * {@code canPlaceAt} is already covered because every Fertilizable
     * spread route goes through this vanilla method.
     */
    @WrapMethod(method = "useOnFertilizable")
    private static boolean placeable$wrapUseOnFertilizable(
            ItemStack stack, World world, BlockPos pos,
            Operation<Boolean> original) {
        Placeable.enterNaturalTick();
        try {
            return original.call(stack, world, pos);
        } finally {
            Placeable.exitNaturalTick();
        }
    }
}
