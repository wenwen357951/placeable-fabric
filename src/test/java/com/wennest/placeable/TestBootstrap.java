package com.wennest.placeable;

import net.minecraft.Bootstrap;
import net.minecraft.SharedConstants;

/**
 * One-shot Minecraft bootstrap helper for unit tests.
 *
 * <p>The test classpath includes {@code fabric-loader-junit}, which installs
 * Loader's {@code knot} classloader before JUnit runs. That solves the
 * package-private Yarn accessor issue (e.g., {@code SimpleRegistry.add}
 * throwing {@link IllegalAccessError}) — but it does NOT invoke
 * {@link Bootstrap#initialize()}. The first test to touch {@code Blocks.*}
 * therefore still trips
 * {@code IllegalArgumentException: Not bootstrapped (called from registry
 * minecraft:game_event)} from {@link net.minecraft.registry.Registries}.
 *
 * <p>This helper bridges that gap: a single static initializer runs the two
 * canonical bootstrap calls. With the knot classloader already in place
 * those calls succeed unconditionally.
 */
final class TestBootstrap {
    static {
        // Sets `SharedConstants.gameVersion`; mandatory before
        // Bootstrap.initialize() because Bootstrap reads it to populate
        // Registries metadata.
        SharedConstants.createGameVersion();
        // Walks the full vanilla bootstrap chain — registries, sound events,
        // particle types, etc. After this returns, every `Blocks.*` static
        // field is safe to dereference from JUnit threads.
        Bootstrap.initialize();
    }

    private TestBootstrap() {
    }

    /**
     * Touch this method from test setup ({@code @BeforeAll}) to force the
     * static initializer above to run exactly once per test JVM.
     */
    static void ensureMinecraftBootstrapped() {
        // Empty body on purpose — referencing the class triggers <clinit>.
    }
}
