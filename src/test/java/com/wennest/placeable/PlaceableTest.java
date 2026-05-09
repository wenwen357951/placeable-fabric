package com.wennest.placeable;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link Placeable}'s pure-logic helpers.
 *
 * <p>The ThreadLocal natural-tick gate has explicit nesting AND
 * exception-safety tests so any regression in the
 * {@code enterNaturalTick} / {@code exitNaturalTick} contract surfaces in
 * CI. These tests deliberately avoid touching MC's {@code Bootstrap} so they
 * run in milliseconds and don't depend on the merged Yarn-mapped jar.
 */
class PlaceableTest {

    /**
     * Drain the natural-tick depth between tests so a misbehaving test can't
     * leak state into the next one. The implementation tolerates over-call.
     */
    @AfterEach
    void resetDepth() {
        // Best-effort cleanup. Five exits cover any reasonable nesting depth
        // a test might have left in place; isNaturalTick() must be false at
        // method exit.
        for (int i = 0; i < 5; i++) {
            if (Placeable.isNaturalTick()) Placeable.exitNaturalTick();
            else break;
        }
        assertFalse(Placeable.isNaturalTick(), "depth must drain between tests");
    }

    // ---- Nesting & basic semantics ----

    @Test
    void naturalTickDepth_startsAtZero() {
        assertFalse(Placeable.isNaturalTick(),
                "fresh thread must start with depth 0");
    }

    @Test
    void naturalTickDepth_nestsCorrectly_zeroToOneToTwoBackToZero() {
        assertFalse(Placeable.isNaturalTick(), "pre: 0");
        Placeable.enterNaturalTick();
        assertTrue(Placeable.isNaturalTick(), "after enter #1: depth 1");
        Placeable.enterNaturalTick();
        assertTrue(Placeable.isNaturalTick(), "after enter #2: depth 2");
        Placeable.exitNaturalTick();
        assertTrue(Placeable.isNaturalTick(),
                "after exit #1: depth still 1, gate still on");
        Placeable.exitNaturalTick();
        assertFalse(Placeable.isNaturalTick(),
                "after exit #2: depth 0, gate off");
    }

    @Test
    void exitOnZeroDepth_isHarmless() {
        // Defensive: the implementation floors at 0 via remove(). Over-exit
        // shouldn't blow up or push depth negative — both would corrupt the
        // gate semantics.
        Placeable.exitNaturalTick();
        assertFalse(Placeable.isNaturalTick());
    }

    // ---- shouldBypass behaviour matrix ----

    @Test
    void shouldBypass_nonWorldWorldView_returnsTrue() {
        // A pure WorldView mock does not implement World — this simulates
        // worldgen's ChunkRegion which extends StructureWorldAccess but
        // not World.
        WorldView fakeWorldgen = mock(WorldView.class);
        BlockPos pos = mock(BlockPos.class);
        assertTrue(Placeable.shouldBypass(fakeWorldgen, pos),
                "non-World WorldView must bypass (worldgen guard)");
    }

    @Test
    void shouldBypass_worldButNaturalTickDepthSet_returnsTrue() {
        // mock(World.class) gives a World subtype mock so the instanceof
        // branch falls through to the natural-tick check.
        World world = mock(World.class);
        BlockPos pos = mock(BlockPos.class);
        Placeable.enterNaturalTick();
        try {
            assertTrue(Placeable.shouldBypass(world, pos),
                    "World+naturalTick must bypass");
        } finally {
            Placeable.exitNaturalTick();
        }
    }

    @Test
    void shouldBypass_worldOutsideNaturalTick_returnsFalse() {
        World world = mock(World.class);
        BlockPos pos = mock(BlockPos.class);
        assertFalse(Placeable.shouldBypass(world, pos),
                "player action on a World subtype must NOT bypass");
    }

    // ---- Exception safety / try-finally bracket emulation ----

    @Test
    void exceptionInBracketedFrame_stillRestoresDepth() {
        // Emulates how AbstractBlockStateNaturalTickMixin@WrapMethod uses
        // try/finally. If an exception escapes vanilla code mid-tick, the
        // depth MUST still be decremented or every subsequent player place
        // on this thread would mis-classify as "natural tick".
        assertFalse(Placeable.isNaturalTick(), "pre: depth 0");
        RuntimeException expected = assertThrows(RuntimeException.class,
                () -> bracketed(() -> {
                    throw new RuntimeException("boom");
                }));
        assertEquals("boom", expected.getMessage());
        assertFalse(Placeable.isNaturalTick(),
                "post: depth must be restored to 0 even on exception");
    }

    @Test
    void exceptionInNestedBracket_restoresOuterDepthOnly() {
        Placeable.enterNaturalTick();
        try {
            assertTrue(Placeable.isNaturalTick());
            assertThrows(RuntimeException.class,
                    () -> bracketed(() -> {
                        throw new RuntimeException("inner");
                    }));
            // Inner bracket exited via its finally; outer depth still 1.
            assertTrue(Placeable.isNaturalTick(),
                    "outer depth must survive inner exception");
        } finally {
            Placeable.exitNaturalTick();
        }
        assertFalse(Placeable.isNaturalTick());
    }

    /**
     * Test helper that mirrors the {@code @WrapMethod} pattern used by
     * {@code AbstractBlockStateNaturalTickMixin}. Centralised here so test
     * cases share the same try/finally shape the real mixin uses.
     */
    private void bracketed(Runnable body) {
        Placeable.enterNaturalTick();
        try {
            body.run();
        } finally {
            Placeable.exitNaturalTick();
        }
    }

    // ---- BoneMealItemMixin contract ----

    /**
     * Documents the {@code BoneMealItemMixin} contract: the mixin behaves as
     * if its body is wrapped in
     * {@code Placeable.enterNaturalTick(); try { ... } finally { exit }}.
     *
     * <p>This test exercises the same bracket shape via the helper used by
     * {@link AbstractBlockStateNaturalTickMixin} so a regression that breaks
     * the {@code BoneMealItemMixin} bracket (e.g., dropping the
     * {@code finally} or swapping {@code @WrapMethod} for a plain
     * {@code @Inject}) would also fail this test in spirit. A real
     * {@code BoneMealItem} call chain cannot be instantiated here (vanilla
     * classes need a Bootstrap), but the bracket pattern itself is the
     * contract: with depth &gt; 0, every {@code shouldBypass} call returns
     * true on this thread and the mod defers to vanilla.
     */
    @Test
    void boneMealMixinBracket_keepsShouldBypassTrueAcrossInnerCanPlaceAt() {
        World fakeWorld = mock(World.class);
        BlockPos fakePos = mock(BlockPos.class);
        // Pre-condition: outside the bracket, a plain World does NOT bypass
        // — i.e., normal player-driven canPlaceAt calls still get the
        // relaxed rule.
        assertFalse(Placeable.shouldBypass(fakeWorld, fakePos),
                "outside bonemeal bracket: World subtype must not bypass");
        // The bracketed() helper mirrors what BoneMealItemMixin's
        // @WrapMethod does: enter, run vanilla logic, exit (in a finally).
        // Inner calls to shouldBypass must observe natural-tick depth > 0.
        bracketed(() -> assertTrue(Placeable.shouldBypass(fakeWorld, fakePos),
                "inside bonemeal bracket: shouldBypass must return true so "
                        + "vanilla bonemeal spawn logic is bit-identical "
                        + "to vanilla"));
        // Post-condition: depth restored, normal player placement works again.
        assertFalse(Placeable.shouldBypass(fakeWorld, fakePos),
                "after bonemeal bracket: depth must drain to 0");
    }

    // ---- Cross-thread ThreadLocal isolation ----

    /**
     * Proves the natural-tick depth ThreadLocal is per-thread isolated:
     * Thread A entering the bracket MUST NOT make Thread B observe
     * {@code isNaturalTick() == true}. Without per-thread isolation, a
     * single random-tick on the server tick thread would silently disable
     * mod intervention for every other thread that touches a canPlaceAt
     * mixin (worker threads, future async chunk loads, etc.).
     *
     * <p>Three latches give deterministic ordering: (1) "B is alive and
     * ready", (2) "A has entered, B may now check", (3) "B has finished
     * checking, A may now exit and join". A 2-second timeout on each latch
     * keeps a hung test from blocking CI indefinitely; the actual flow is
     * sub-millisecond on a warm JVM.
     */
    @Test
    void naturalTickDepth_isolatedAcrossThreads() throws InterruptedException {
        // Synchronization primitives. Latches give deterministic ordering
        // (A enters; B observes; A exits) without sleep-spinning.
        java.util.concurrent.CountDownLatch bReady =
                new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.CountDownLatch aEntered =
                new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.CountDownLatch bChecked =
                new java.util.concurrent.CountDownLatch(1);
        // Collect Thread B's observation in an array so the assertion can
        // run on the main thread (JUnit's failure reporting does not
        // survive a thrown AssertionError on a non-test thread).
        final boolean[] bSawNaturalTick = new boolean[1];
        final Throwable[] bError = new Throwable[1];

        Thread b = new Thread(() -> {
            try {
                // Signal main: I'm alive, you may have Thread A enter the
                // bracket.
                bReady.countDown();
                // Wait for Thread A to enter its natural-tick frame.
                aEntered.await(2, java.util.concurrent.TimeUnit.SECONDS);
                // The point of the test: Thread B's view of the
                // ThreadLocal MUST be independent of Thread A's. With
                // per-thread storage, bSawNaturalTick stays false even
                // though A's depth is 1.
                bSawNaturalTick[0] = Placeable.isNaturalTick();
            } catch (Throwable t) {
                bError[0] = t;
            } finally {
                bChecked.countDown();
            }
        }, "placeable-test-thread-b");
        b.setDaemon(true);
        b.start();

        // Wait until Thread B has started before manipulating Thread A's
        // (i.e., the main thread's) ThreadLocal — keeps the ordering tight.
        assertTrue(bReady.await(2, java.util.concurrent.TimeUnit.SECONDS),
                "Thread B must start within 2s");

        // Thread A = the main test thread. Enter the bracket, hand off to B.
        Placeable.enterNaturalTick();
        try {
            assertTrue(Placeable.isNaturalTick(),
                    "main thread (A) must see depth > 0 after enterNaturalTick");
            aEntered.countDown();
            assertTrue(bChecked.await(2, java.util.concurrent.TimeUnit.SECONDS),
                    "Thread B must finish checking within 2s");
        } finally {
            // Always exit — same try/finally discipline as the real mixin.
            Placeable.exitNaturalTick();
        }

        b.join(2_000);
        // If Thread B crashed, surface that here — otherwise a silent
        // failure would skip the real assertion below.
        if (bError[0] != null) {
            throw new AssertionError("Thread B threw: " + bError[0], bError[0]);
        }
        assertFalse(bSawNaturalTick[0],
                "Thread B must NOT see Thread A's natural-tick depth — "
                        + "ThreadLocal isolation is the foundation of the "
                        + "natural-tick gate");
        // Sanity: main thread depth back to 0.
        assertFalse(Placeable.isNaturalTick(),
                "main thread depth must drain to 0 after the test");
    }
}
