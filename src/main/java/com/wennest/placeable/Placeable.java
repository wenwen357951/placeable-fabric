package com.wennest.placeable;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigHolder;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ModInitializer;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Static gate for the mod's behavioural overrides.
 *
 * <p>Every {@code canPlaceAt} mixin in {@code com.wennest.placeable.mixin}
 * routes its decision through {@link #shouldBypass(WorldView, BlockPos)}
 * (defer-to-vanilla check) and {@link #isDisabled(BlockState)} (per-plant
 * config check) before injecting any {@code setReturnValue(true)}.
 *
 * <p>Two gates together keep the mod from widening vanilla worldgen or
 * natural ecology spread:
 * <ul>
 *   <li>{@link #shouldBypass(WorldView, BlockPos)} returns {@code true}
 *       during worldgen (callers where {@code world} is a {@code ChunkRegion}
 *       rather than a {@code World} subtype) and inside natural-tick frames
 *       bracketed by {@link #enterNaturalTick()} / {@link #exitNaturalTick()}.</li>
 *   <li>{@link #isDisabled(BlockState)} encodes the user's config toggles.</li>
 * </ul>
 *
 * <p>The mod holds no persistent world state. The only file written is
 * {@code config/placeable.json}, managed by Cloth Config / AutoConfig outside
 * the world save.
 *
 * <h2>Threading</h2>
 *
 * <p>{@code NATURAL_TICK_DEPTH} is a {@link ThreadLocal} so depth tracking is
 * per-thread. Vanilla {@code randomTick} / {@code scheduledTick} run on the
 * server tick thread, but per-thread storage keeps the gate correct on any
 * thread that drives natural-tick logic.
 *
 * <p>This class is static-only; never instantiate it. Fabric Loader builds a
 * singleton from {@code fabric.mod.json} and invokes {@link #onInitialize()}
 * reflectively.
 */
public class Placeable implements ModInitializer {
    public static final String MODID = "placeable";
    public static final Logger LOGGER = LoggerFactory.getLogger(Placeable.class);

    /**
     * Per-thread re-entrant depth counter for natural-tick frames.
     *
     * <p>Incremented on entry to vanilla {@code randomTick} /
     * {@code scheduledTick}; decremented on exit, including exceptional exit
     * (try/finally inside the wrapping mixin).
     *
     * <p>While {@code depth > 0} on the current thread,
     * {@link #shouldBypass(WorldView, BlockPos)} returns {@code true} — any
     * {@code canPlaceAt} call reached from inside vanilla mushroom spread,
     * cactus stack-validation, sugar-cane upward growth, etc., defers to
     * vanilla logic instead of being widened by the mod.
     *
     * <p>Initial value 0; values are GC'd with the owning thread.
     */
    private static final ThreadLocal<Integer> NATURAL_TICK_DEPTH =
            ThreadLocal.withInitial(() -> 0);

    /**
     * Cached config holder, populated once in {@link #onInitialize()}; later
     * reads avoid the {@link AutoConfig#getConfigHolder} class-lookup cost on
     * the hot path.
     *
     * <p>{@code volatile} so the value published by the init thread is visible
     * to the server tick thread without further synchronization.
     */
    private static volatile ConfigHolder<PlaceableConfig> CONFIG_HOLDER;

    /**
     * Single-shot warn-once flag for {@link #getConfig()} early-init misses.
     * The volatile write means a second thread that observes {@code true} can
     * skip the WARN log without taking a lock; the worst case is two threads
     * both logging the WARN before the flag is set, which is harmless.
     */
    private static volatile boolean configLogWarned = false;

    /**
     * Increment the natural-tick depth on the current thread. MUST be paired
     * with {@link #exitNaturalTick()} via {@code try/finally} (or
     * MixinExtras' {@code @WrapMethod}) so the counter never leaks if vanilla
     * code throws.
     */
    public static void enterNaturalTick() {
        NATURAL_TICK_DEPTH.set(NATURAL_TICK_DEPTH.get() + 1);
    }

    /**
     * Decrement the natural-tick depth on the current thread. Idempotent floor
     * at 0 — defensive against accidental over-call. A leaked depth would
     * cascade into "every player placement is treated as worldgen", so the
     * floor exists as cheap insurance.
     */
    public static void exitNaturalTick() {
        int current = NATURAL_TICK_DEPTH.get();
        if (current <= 1) {
            // Drop to zero; remove() releases the ThreadLocal slot back to the
            // JVM's per-thread map, avoiding long-term memory pressure on
            // long-lived server threads.
            NATURAL_TICK_DEPTH.remove();
        } else {
            NATURAL_TICK_DEPTH.set(current - 1);
        }
    }

    /**
     * @return {@code true} iff the current thread is currently executing
     * inside a vanilla {@code randomTick} / {@code scheduledTick}
     * frame (depth &gt; 0).
     */
    public static boolean isNaturalTick() {
        return NATURAL_TICK_DEPTH.get() > 0;
    }

    /**
     * Master gate: should the mod step aside and let vanilla decide?
     *
     * <p>Returns {@code true} when:
     * <ul>
     *   <li>{@code world} is not a runtime {@link World} subtype — i.e., it's
     *       a {@code ChunkRegion} from worldgen.</li>
     *   <li>The current thread is inside a vanilla natural-tick frame.</li>
     * </ul>
     *
     * <p>Every {@code canPlaceAt} mixin MUST call this <em>first</em>, before
     * any {@code setReturnValue(true)}. Defer to vanilla during worldgen and
     * natural ticks to avoid widening vanilla plant placement and natural
     * ecology spread.
     *
     * <p>Hot path — keep allocation-free. Two field accesses plus one
     * {@link ThreadLocal#get()} stays well under the {@code canPlaceAt}
     * performance budget.
     *
     * @param world calling world (worldgen passes {@code ChunkRegion}; runtime
     *              passes {@code ServerWorld} / {@code ClientWorld}).
     * @param pos   reference position; currently unused but reserved for
     *              future per-position gates (e.g., dimension-specific opt-out).
     * @return {@code true} if the mod MUST defer to vanilla.
     */
    public static boolean shouldBypass(WorldView world, BlockPos pos) {
        // ChunkRegion (worldgen) does NOT extend World. ServerWorld /
        // ClientWorld DO extend World. The instanceof check is therefore the
        // exact predicate for "worldgen vs runtime".
        if (!(world instanceof World)) return true;
        // Even at runtime, vanilla natural-tick frames must defer.
        return isNaturalTick();
    }

    /**
     * Floor-validity rule used by every {@code canPlaceAt} mixin to decide
     * whether the block <em>below</em> the plant counts as a relaxed-rule
     * placement surface.
     *
     * <p>This two-argument overload takes the <em>plant's own position</em>.
     * Internally it computes {@code pos.down()} — i.e., the floor block
     * directly under the plant — and delegates to
     * {@link #isValidFloor(BlockState, BlockView, BlockPos)}. Mixins call this
     * overload from inside {@code canPlaceAt}, where the mixin already has the
     * plant's {@code BlockPos pos} parameter and would otherwise need to
     * redundantly compute the floor position itself.
     *
     * <p>Hard reject: an {@linkplain BlockState#isAir() air} floor is never
     * accepted, regardless of any other config flag. Without this preempt the
     * {@code placedWithoutTopRim=true} setting would short-circuit to
     * {@code true} and the player could place plants in mid-air.
     *
     * <p>Three accept conditions, evaluated in order (after the air-reject):
     * <ol>
     *   <li>{@link Block#hasTopRim} — the floor has a flat top (full block,
     *       slab top-half, stair top-half, fence-post cap, etc.). Skipped if
     *       {@link PlaceableConfig#placedWithoutTopRim} is {@code true}, in
     *       which case any non-air floor passes.</li>
     *   <li>The floor is in {@link BlockTags#LEAVES}.</li>
     *   <li>The floor is {@link Blocks#DIRT_PATH}.</li>
     * </ol>
     *
     * <p>This method does NOT itself decide whether the mod should intervene.
     * It is the <em>terminal</em> rule, used after {@link #shouldBypass} has
     * cleared the call. Worldgen and natural-tick gating happens upstream;
     * this overload simply answers "given that the mod is allowed to act,
     * does this floor qualify as a relaxed-rule surface?".
     *
     * @param world world view (any subtype).
     * @param pos   the <em>plant's own position</em>; the floor is at
     *              {@code pos.down()}. Passing the floor position here would
     *              produce off-by-one results — use the three-argument
     *              overload if you already have the floor position.
     * @return {@code true} iff the floor qualifies under at least one of the
     * three accept conditions above.
     */
    public static boolean isValidFloor(WorldView world, BlockPos pos) {
        return isValidFloor(world.getBlockState(pos.down()), world, pos.down());
    }

    /**
     * Floor-validity rule with an explicit floor BlockState.
     *
     * <p>Unlike the two-argument overload, every parameter of this method
     * describes the <em>floor</em>, not the plant:
     * <ul>
     *   <li>{@code floor} — the BlockState of the block directly under the
     *       plant.</li>
     *   <li>{@code world} — a {@link BlockView} of the floor's chunk; used
     *       by {@link Block#hasTopRim} to consult the floor block's shape.</li>
     *   <li>{@code pos} — the <em>floor's own position</em>, i.e., the plant's
     *       position minus one Y unit. Passing the plant position here would
     *       cause {@code Block#hasTopRim} to consult a one-block-too-high
     *       shape and silently reject every otherwise-valid placement.</li>
     * </ul>
     *
     * <p>Callers that already hold a {@code BlockState} for the floor should
     * prefer this overload to avoid a redundant {@code world.getBlockState}
     * lookup. Callers that only have the plant position should use the
     * two-argument overload.
     *
     * <p>Same deferral applies as for the two-argument overload — this method
     * assumes {@link #shouldBypass} has already cleared the call.
     *
     * @param floor the BlockState of the floor (one block below the plant).
     * @param world world view (used by {@link Block#hasTopRim} for shape lookup).
     * @param pos   the <em>floor's own position</em>, NOT the plant's.
     * @return {@code true} iff the floor qualifies under at least one of the
     * three accept conditions documented on the two-argument overload.
     */
    public static boolean isValidFloor(BlockState floor, BlockView world, BlockPos pos) {
        // Air-floor hard reject. With placedWithoutTopRim=true the OR-chain
        // below would otherwise short-circuit to true regardless of what
        // `floor` is, including air — which would let the player place plants
        // in mid-air. Even in the most permissive config a plant must have
        // SOMETHING beneath it.
        if (floor.isAir()) return false;

        PlaceableConfig config = getConfig();
        // getConfig() can return null briefly during early init. Treat that
        // as "use defaults" — the default for floor checks requires a top rim.
        boolean withoutTopRim = config != null && config.placedWithoutTopRim;
        return (withoutTopRim || Block.hasTopRim(world, pos))
                || floor.isIn(BlockTags.LEAVES)
                || floor.isOf(Blocks.DIRT_PATH);
    }

    /**
     * Should the mod abstain from intervening for this BlockState? Returns
     * {@code true} iff (a) the mod is globally disabled, (b) the block is not
     * in the placeable catalogue, or (c) the user has toggled this specific
     * plant off.
     */
    public static boolean isDisabled(BlockState blockState) {
        return isDisabled(blockState.getBlock());
    }

    /**
     * Convenience overload: read the BlockState from the world first.
     */
    public static boolean isDisabled(WorldView world, BlockPos blockPos) {
        return isDisabled(world.getBlockState(blockPos));
    }

    /**
     * Core config-driven gate. Routes the lookup through
     * {@link PlaceablePlants#findBy(Block)} which is O(1).
     */
    public static boolean isDisabled(Block block) {
        PlaceableConfig config = getConfig();
        // Treat null config as "mod disabled" — the safest default.
        if (config == null || !config.enable) {
            return true;
        }
        Optional<PlaceablePlants> placeablePlants = PlaceablePlants.findBy(block);
        if (placeablePlants.isEmpty()) {
            return true;
        }
        // Use the read-side helper rather than direct getOrDefault so the
        // null-key fallback (default-true semantics) lives in one place.
        return !config.isPlantAllowed(placeablePlants.get());
    }

    /**
     * Cached, null-safe accessor for the active config. Prefers the cached
     * {@link #CONFIG_HOLDER} populated in {@link #onInitialize()}; falls back
     * to a direct {@link AutoConfig#getConfigHolder} lookup if the cache
     * hasn't been populated yet (e.g., a mixin fires before {@code onInitialize}
     * — defensive against ordering surprises across mod loaders).
     *
     * <p>Returns {@code null} if AutoConfig itself has not been registered
     * yet; callers MUST handle the null case (the mod treats null as
     * "disabled" — see {@link #isDisabled(Block)}).
     *
     * <p>Reads may briefly observe {@code null} even after another thread has
     * finished initialising the holder. The {@code volatile} field publishes
     * the new reference eventually, but a reader currently inside the
     * {@code holder == null} branch may have loaded the field before the
     * writer's store. Callers MUST default-deny on null. The window lasts
     * only across one init tick.
     */
    public static PlaceableConfig getConfig() {
        ConfigHolder<PlaceableConfig> holder = CONFIG_HOLDER;
        if (holder == null) {
            try {
                holder = AutoConfig.getConfigHolder(PlaceableConfig.class);
                CONFIG_HOLDER = holder;
            } catch (IllegalStateException e) {
                // AutoConfig.getConfigHolder throws IllegalStateException when
                // the class hasn't been registered yet — happens in unit tests
                // and during very early classloading before onInitialize fires.
                // Catch the specific exception type so unrelated runtime
                // failures (NPE, ClassCastException) propagate instead of
                // being silently swallowed as "mod disabled".
                //
                // Log at WARN once per JVM run to make the early-init fall-
                // through visible without spamming the log on every tick.
                if (!configLogWarned) {
                    configLogWarned = true;
                    LOGGER.warn(
                            "Placeable.getConfig(): AutoConfig holder not yet "
                                    + "registered; defaulting to disabled. "
                                    + "Subsequent calls will populate the cache "
                                    + "once onInitialize completes.",
                            e);
                }
                return null;
            }
        }
        return holder == null ? null : holder.get();
    }

    @Override
    public void onInitialize() {
        long loadTook = System.currentTimeMillis();
        // Register and immediately cache the holder. The cached reference
        // means subsequent reads bypass AutoConfig's internal class-keyed
        // lookup, which matters because canPlaceAt mixins may call
        // getConfig() thousands of times per second.
        CONFIG_HOLDER = AutoConfig.register(PlaceableConfig.class, GsonConfigSerializer::new);
        // Run upgrade-safety / orphan-cleanup on the loaded config. Doing this
        // once at init keeps the hot read path branch-free of reconciliation.
        PlaceableConfig cfg = CONFIG_HOLDER.get();
        if (cfg != null) {
            cfg.validatePostLoad();
            CONFIG_HOLDER.save();
        }
        LOGGER.info("Mod loaded in {} ms!", System.currentTimeMillis() - loadTook);
    }
}
