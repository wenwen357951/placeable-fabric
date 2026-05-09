package com.wennest.placeable;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for the {@link PlaceablePlants} O(1) lookup table.
 *
 * <p>Bootstrap note: {@code fabric-loader-junit} installs Loader's
 * {@code knot} classloader so Yarn package-private accessors are visible to
 * tests. {@link TestBootstrap} additionally invokes
 * {@code SharedConstants.createGameVersion()} +
 * {@code Bootstrap.initialize()} once per JVM in {@code @BeforeAll},
 * satisfying the {@code Registries.ensureBootstrapped} guard before any
 * {@code Blocks.*} dereference. The "unknown block" branch uses a Mockito
 * mock to avoid coupling the assertion to a specific non-catalogued vanilla
 * {@link Block}.
 */
class PlaceablePlantsTest {

    @BeforeAll
    static void bootstrapMinecraft() {
        // See PlaceableConfigTest#bootstrapMinecraft — runs the bootstrap
        // chain exactly once per JVM (TestBootstrap is idempotent via its
        // static initializer).
        TestBootstrap.ensureMinecraftBootstrapped();
    }

    // ---- findBy contract ----

    @Test
    void findBy_knownBlock_returnsMatchingEntry() {
        // OAK_SAPLING is one of the catalogued plants; round-trip through
        // findBy must return the same enum entry.
        Block known = PlaceablePlants.OAK_SAPLING.getBlock();
        Optional<PlaceablePlants> result = PlaceablePlants.findBy(known);
        assertTrue(result.isPresent(), "known block must resolve");
        assertSame(PlaceablePlants.OAK_SAPLING, result.get(),
                "the resolved entry must be the same enum constant");
    }

    @Test
    void findBy_unknownBlock_returnsEmpty() {
        // A pure Mockito mock of Block is guaranteed not to be one of the
        // catalogued vanilla Blocks instances.
        Block stranger = mock(Block.class);
        Optional<PlaceablePlants> result = PlaceablePlants.findBy(stranger);
        assertTrue(result.isEmpty(), "unknown block must resolve to empty");
    }

    @Test
    void findBy_null_returnsEmpty() {
        // Documented contract: null in -> Optional.empty() out, NOT NPE.
        Optional<PlaceablePlants> result = PlaceablePlants.findBy(null);
        assertTrue(result.isEmpty(), "null block must resolve to empty");
    }

    // ---- BY_BLOCK lookup-table consistency ----

    @Test
    void byBlockMap_isConsistentWithEnumValues() {
        Map<Block, PlaceablePlants> view = PlaceablePlants.byBlockView();
        assertEquals(PlaceablePlants.values().length, view.size(),
                "BY_BLOCK must have one entry per enum value " +
                        "(no duplicate-block collisions)");
        for (PlaceablePlants p : PlaceablePlants.values()) {
            assertNotNull(p.getBlock(), "every enum entry must have a non-null block");
            assertSame(p, view.get(p.getBlock()),
                    "BY_BLOCK[block] must resolve to the originating enum value");
        }
    }

    @Test
    void byBlockMap_isUnmodifiable() {
        Map<Block, PlaceablePlants> view = PlaceablePlants.byBlockView();
        Block stranger = mock(Block.class);
        boolean threw;
        try {
            view.put(stranger, PlaceablePlants.OAK_SAPLING);
            threw = false;
        } catch (UnsupportedOperationException expected) {
            threw = true;
        }
        assertTrue(threw, "BY_BLOCK view must be immutable");
    }

    // ---- LEAF_LITTER coverage on 1.21.5+ ----

    /**
     * LEAF_LITTER (added in 1.21.5 "Spring to Life") must be in the
     * placeable catalogue so that {@code LeafLitterBlockMixin}'s
     * {@code Placeable.isDisabled} gate flips correctly.
     *
     * <p>Stonecutter strips the entire test method on 1.21.1 / 1.21.4
     * builds (where {@code Blocks.LEAF_LITTER} does not exist) — the
     * {@code //? if >=1.21.5} block-scope wraps the import, the
     * {@code @Test} annotation, the method declaration, and its body.
     */
    //? if >=1.21.5 {
    @Test
    void findBy_leafLitter_isPresentOn1_21_5Plus() {
        Optional<PlaceablePlants> result = PlaceablePlants.findBy(Blocks.LEAF_LITTER);
        assertTrue(result.isPresent(),
                "LEAF_LITTER must be a placeable plant on 1.21.5+");
        assertSame(PlaceablePlants.LEAF_LITTER, result.get(),
                "lookup of Blocks.LEAF_LITTER must resolve to the LEAF_LITTER enum entry");
    }
    //?}
}
