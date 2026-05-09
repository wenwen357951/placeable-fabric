package com.wennest.placeable;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link Placeable#isValidFloor(BlockState, BlockView, BlockPos)}.
 *
 * <p>The relaxed-floor rule short-circuits to {@code true} when
 * {@link PlaceableConfig#placedWithoutTopRim} is enabled. Without an explicit
 * air-floor reject, that short-circuit also fires for an air floor — which
 * would let the player place plants in mid-air. These tests pin the
 * air-reject behaviour so a future regression that drops the air-guard fails
 * CI.
 *
 * <p><b>Bootstrap requirements</b>: like {@link PlaceablePlantsTest}, this
 * fixture relies on {@link TestBootstrap} for the {@code SharedConstants} /
 * {@code Bootstrap} initialization; on top of that it registers AutoConfig
 * once so {@link Placeable#getConfig()} returns a real config instance the
 * test can flip {@code placedWithoutTopRim} on. Without the registration,
 * {@code getConfig()} returns {@code null} (treated as
 * {@code placedWithoutTopRim=false}) and the most permissive scenario
 * cannot be exercised.
 */
class IsValidFloorTest {

    /**
     * Cached config instance whose flags the tests mutate.
     */
    private static PlaceableConfig CONFIG;

    @BeforeAll
    static void bootstrapAndRegisterConfig() {
        // Force the MC bootstrap chain so Blocks.AIR / Blocks.STONE / etc.
        // are safe to dereference. Same idempotent-static-init pattern the
        // other tests use.
        TestBootstrap.ensureMinecraftBootstrapped();
        // AutoConfig.register is idempotent at the class level: a second call
        // throws RuntimeException("Config ... already registered"). Guard it
        // so multiple test classes (PlaceableConfigTest, this one, ...) can
        // coexist in the same JVM.
        try {
            AutoConfig.register(PlaceableConfig.class, GsonConfigSerializer::new);
        } catch (RuntimeException alreadyRegistered) {
            // Expected if another test class registered first; the holder
            // fetched below is the same singleton.
        }
        CONFIG = AutoConfig.getConfigHolder(PlaceableConfig.class).get();
        // Run the post-load reconciliation so the map is populated with the
        // current enum keyset (mirrors what Placeable.onInitialize does in
        // production).
        CONFIG.validatePostLoad();
    }

    @AfterAll
    static void resetConfigState() {
        // Defensive: leave the shared singleton in its default state so a
        // subsequent test class doesn't inherit the flipped flags.
        if (CONFIG != null) {
            CONFIG.placedWithoutTopRim = false;
        }
    }

    @BeforeEach
    void resetConfig() {
        // Each test starts with the default config (placedWithoutTopRim=false)
        // and opts in to permissive mode where it needs to.
        CONFIG.placedWithoutTopRim = false;
    }

    // ---- Air-floor reject (the headline behaviour) ----

    /**
     * Headline regression test: even with the most permissive
     * {@code placedWithoutTopRim=true} setting, an air floor must NOT be
     * accepted. Without the air-reject early-return, the OR-chain would
     * short-circuit on {@code (true || ...) || ... || ...} and the user
     * could place plants in mid-air.
     */
    @Test
    void isValidFloor_returnsFalse_whenFloorIsAir_evenWithPlacedWithoutTopRim() {
        CONFIG.placedWithoutTopRim = true;
        BlockState air = Blocks.AIR.getDefaultState();
        // BlockView and BlockPos are not consulted on the air branch — the
        // method should reject before ever calling Block.hasTopRim — so a
        // pure mock is sufficient and intentional (proves the air-reject
        // happens BEFORE the world-touching path).
        BlockView world = mock(BlockView.class);
        BlockPos pos = mock(BlockPos.class);
        assertFalse(Placeable.isValidFloor(air, world, pos),
                "air floor must NEVER be a valid placement surface, even "
                        + "with placedWithoutTopRim=true");
    }

    /**
     * Companion case: air must be rejected even with the default
     * {@code placedWithoutTopRim=false}. Ensures the early-return doesn't
     * regress when the OR-chain would have rejected anyway.
     */
    @Test
    void isValidFloor_returnsFalse_whenFloorIsAir_withDefaultConfig() {
        // Default: placedWithoutTopRim=false (set by @BeforeEach).
        BlockState air = Blocks.AIR.getDefaultState();
        BlockView world = mock(BlockView.class);
        BlockPos pos = mock(BlockPos.class);
        assertFalse(Placeable.isValidFloor(air, world, pos),
                "air floor must always be rejected regardless of config flag");
    }

    // ---- Sanity baselines: the three accept conditions ----

    /**
     * A standard top-rim block (stone) must pass the floor check on the
     * default config. This is the vanilla-equivalent baseline — without it
     * the relaxed-rule mod has nothing to relax.
     */
    @Test
    void isValidFloor_returnsTrue_whenFloorHasTopRim() {
        BlockState stone = Blocks.STONE.getDefaultState();
        // Block.hasTopRim consults the world view to read the block's
        // collision shape. Use a real (origin) BlockPos so it isn't a mock
        // that might NPE inside Block#hasTopRim.
        BlockPos origin = BlockPos.ORIGIN;
        // Build a lightweight BlockView that returns the stone state at
        // origin so hasTopRim can probe its shape. Mockito's default-answer
        // strategy returns null/0 for unmocked methods which would NPE
        // inside hasTopRim's shape lookup; stub the minimum.
        BlockView world = mock(BlockView.class);
        org.mockito.Mockito.when(world.getBlockState(origin)).thenReturn(stone);
        assertTrue(Placeable.isValidFloor(stone, world, origin),
                "a full top-rim block (stone) must be a valid floor");
    }

    /**
     * Permissive baseline: with {@code placedWithoutTopRim=true} the
     * {@code (withoutTopRim || hasTopRim)} branch short-circuits to true on
     * the FIRST OR-clause for any non-air floor — no need to consult
     * {@link net.minecraft.block.Block#hasTopRim} (which would touch the
     * BlockView) or the {@code BlockTags.LEAVES} predicate (which would
     * need the tag registry to be bound). Using {@code Blocks.STONE} as a
     * representative non-air floor keeps the test independent of tag
     * binding while still proving the relaxed-floor rule works once the
     * air-reject is cleared.
     *
     * <p>Note: dedicated leaves / dirt-path baselines are not included here
     * because both branches require {@code BlockTags} binding which the
     * {@link TestBootstrap} chain does not establish in a JUnit JVM. Those
     * branches are exercised via {@code runClient} integration testing.
     */
    @Test
    void isValidFloor_returnsTrue_anyNonAirFloor_whenPlacedWithoutTopRim() {
        CONFIG.placedWithoutTopRim = true;
        BlockState stone = Blocks.STONE.getDefaultState();
        // No need to stub world.getBlockState — placedWithoutTopRim=true
        // means the (withoutTopRim || hasTopRim) clause short-circuits on
        // `withoutTopRim` first; Block.hasTopRim is never invoked.
        BlockView world = mock(BlockView.class);
        BlockPos pos = mock(BlockPos.class);
        assertTrue(Placeable.isValidFloor(stone, world, pos),
                "with placedWithoutTopRim=true any non-air floor must pass");
    }
}
