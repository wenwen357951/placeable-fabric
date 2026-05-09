package com.wennest.placeable;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link PlaceableConfig#validatePostLoad()} and the
 * concurrent map storage chosen for thread-safety.
 *
 * <p>Bootstrap note: the test classpath includes {@code fabric-loader-junit},
 * which installs Loader's {@code knot} classloader so Yarn's package-private
 * accessors are visible to test code. {@link TestBootstrap} additionally
 * drives {@code SharedConstants.createGameVersion()} +
 * {@code Bootstrap.initialize()} once per JVM, satisfying the
 * {@code Registries.ensureBootstrapped} guard that fires the moment
 * {@code Blocks.*} touches the registry chain.
 */
class PlaceableConfigTest {

    @BeforeAll
    static void bootstrapMinecraft() {
        // Force the TestBootstrap <clinit> to run exactly once per JVM. With
        // fabric-loader-junit on the classpath this call is guaranteed to
        // succeed.
        TestBootstrap.ensureMinecraftBootstrapped();
    }

    @Test
    void validatePostLoad_fillsMissingEnumKeysWithTrue() {
        PlaceableConfig cfg = new PlaceableConfig();
        // Simulate an old config file: drop a couple of recently-added enum
        // keys to mimic the upgrade scenario.
        cfg.allowPlaceablePlants.remove(PlaceablePlants.WHEAT);
        cfg.allowPlaceablePlants.remove(PlaceablePlants.OAK_SAPLING);

        cfg.validatePostLoad();

        assertEquals(Boolean.TRUE, cfg.allowPlaceablePlants.get(PlaceablePlants.WHEAT),
                "missing key WHEAT must default to true");
        assertEquals(Boolean.TRUE, cfg.allowPlaceablePlants.get(PlaceablePlants.OAK_SAPLING),
                "missing key OAK_SAPLING must default to true");
    }

    @Test
    void validatePostLoad_preservesUserDisables() {
        PlaceableConfig cfg = new PlaceableConfig();
        cfg.allowPlaceablePlants.put(PlaceablePlants.WHEAT, Boolean.FALSE);
        cfg.validatePostLoad();
        assertEquals(Boolean.FALSE, cfg.allowPlaceablePlants.get(PlaceablePlants.WHEAT),
                "validatePostLoad must NOT overwrite user-set false");
    }

    @Test
    void validatePostLoad_dropsOrphanNullKey() {
        PlaceableConfig cfg = new PlaceableConfig();
        // Gson stores unknown enum values under null when deserializing old
        // files referencing renamed/removed entries. ConcurrentHashMap rejects
        // null keys, so simulate with a HashMap and let validatePostLoad
        // migrate it.
        Map<PlaceablePlants, Boolean> withNull = new HashMap<>();
        withNull.put(null, Boolean.TRUE);
        withNull.put(PlaceablePlants.WHEAT, Boolean.TRUE);
        cfg.allowPlaceablePlants = withNull;

        cfg.validatePostLoad();

        // The post-validate storage is a ConcurrentHashMap, which by JDK
        // contract rejects null keys with an NPE on get/containsKey.
        // Asserting .get(null) directly would therefore throw rather than
        // return null, so verify orphan removal indirectly: the map must
        // contain exactly one entry per enum constant — no extra null slot.
        assertEquals(PlaceablePlants.values().length, cfg.allowPlaceablePlants.size(),
                "orphan null key must be removed (size must equal enum count)");
        assertEquals(Boolean.TRUE, cfg.allowPlaceablePlants.get(PlaceablePlants.WHEAT));
        assertTrue(cfg.allowPlaceablePlants instanceof ConcurrentHashMap,
                "validatePostLoad must promote storage to ConcurrentHashMap");
    }

    @Test
    void validatePostLoad_isIdempotent() {
        PlaceableConfig cfg = new PlaceableConfig();
        cfg.validatePostLoad();
        int sizeAfterFirst = cfg.allowPlaceablePlants.size();
        cfg.validatePostLoad();
        int sizeAfterSecond = cfg.allowPlaceablePlants.size();
        assertEquals(sizeAfterFirst, sizeAfterSecond,
                "validatePostLoad must be idempotent");
        assertEquals(PlaceablePlants.values().length, sizeAfterFirst,
                "after validate, map must contain exactly enum.values().length keys");
    }

    @Test
    void isPlantAllowed_defaultsTrueOnMissingKey() {
        PlaceableConfig cfg = new PlaceableConfig();
        cfg.allowPlaceablePlants.remove(PlaceablePlants.WHEAT);
        // Don't call validatePostLoad — this models the early-init window
        // before validation has run; the read-side helper must still be safe.
        assertTrue(cfg.isPlantAllowed(PlaceablePlants.WHEAT),
                "isPlantAllowed must default-true for missing keys");
    }

    @Test
    void isPlantAllowed_returnsFalseForUserDisabled() {
        PlaceableConfig cfg = new PlaceableConfig();
        cfg.allowPlaceablePlants.put(PlaceablePlants.WHEAT, Boolean.FALSE);
        assertFalse(cfg.isPlantAllowed(PlaceablePlants.WHEAT));
    }

    @Test
    void concurrentReadWrite_doesNotThrow() throws InterruptedException {
        // A reader on the server tick thread must never see a
        // ConcurrentModificationException while the render thread (Mod Menu
        // UI) writes config toggles. This is a smoke test for that
        // invariant — it doesn't prove absence of CMEs but it's a strong
        // signal if the storage type were to regress to a plain HashMap.
        final PlaceableConfig cfg = new PlaceableConfig();
        cfg.validatePostLoad();
        final int iterations = 5_000;
        final CountDownLatch start = new CountDownLatch(1);
        final AtomicReference<Throwable> writerErr = new AtomicReference<>();
        final AtomicReference<Throwable> readerErr = new AtomicReference<>();

        Thread writer = new Thread(() -> {
            try {
                start.await();
                for (int i = 0; i < iterations; i++) {
                    boolean v = (i & 1) == 0;
                    cfg.allowPlaceablePlants.put(PlaceablePlants.WHEAT, v);
                    cfg.allowPlaceablePlants.put(PlaceablePlants.OAK_SAPLING, !v);
                }
            } catch (Throwable t) {
                writerErr.set(t);
            }
        }, "config-writer");

        Thread reader = new Thread(() -> {
            try {
                start.await();
                for (int i = 0; i < iterations; i++) {
                    // Iterate the map; this is what Mod Menu's "list every
                    // toggle" code path does on the render thread.
                    for (Map.Entry<PlaceablePlants, Boolean> e :
                            cfg.allowPlaceablePlants.entrySet()) {
                        // touch entries so the JIT doesn't elide the loop.
                        if (e.getKey() == null) writerErr.set(new IllegalStateException("null key"));
                    }
                    cfg.isPlantAllowed(PlaceablePlants.WHEAT);
                }
            } catch (Throwable t) {
                readerErr.set(t);
            }
        }, "config-reader");

        writer.start();
        reader.start();
        start.countDown();
        writer.join(TimeUnit.SECONDS.toMillis(10));
        reader.join(TimeUnit.SECONDS.toMillis(10));

        assertNull(writerErr.get(), "writer must not throw");
        assertNull(readerErr.get(), "reader must not throw (no CME)");
    }
}
