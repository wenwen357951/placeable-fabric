// Settings script — root configuration for the Placeable Plants Gradle build.
//
// Stonecutter (https://stonecutter.kikugie.dev) drives multi-version support
// for this mod. The single source tree under `src/main/` is preprocessed per
// Minecraft version using `//? if` comments inside Java sources. Each
// declared version below becomes a Gradle subproject backed by the shared
// `build.gradle.kts` (centralScript pattern).
//
// Branch-based multi-version support is unsustainable past three versions;
// preprocessor-based support is the community standard for Fabric mods,
// hence Stonecutter.
//
// Version-specific overrides live in `versions/<mc-version>/gradle.properties`.

pluginManagement {
    repositories {
        // Fabric ecosystem (Loom plugin, Yarn metadata).
        maven("https://maven.fabricmc.net/")
        // Stonecutter is hosted on KikuGie's Maven.
        maven("https://maven.kikugie.dev/releases") {
            name = "KikuGie Releases"
        }
        // Snapshots channel — kept available for future pre-release plugin testing.
        maven("https://maven.kikugie.dev/snapshots") {
            name = "KikuGie Snapshots"
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    // Stonecutter 0.9.3 — see https://stonecutter.kikugie.dev for full
    // documentation. Pin is intentional: Stonecutter's API is still pre-1.0
    // and minor versions can make breaking changes. Bumping requires a manual
    // review of stonecutter.gradle.kts and settings.gradle.kts.
    id("dev.kikugie.stonecutter") version "0.9.3"
}

stonecutter {
    // `centralScript = true` means every chiseled subproject reuses the root
    // `build.gradle.kts`. There are no per-version build scripts;
    // cross-version differences are expressed via `//? if` comments in source
    // files only.
    centralScript = "build.gradle.kts"

    create(rootProject) {
        // Target Minecraft versions for the Stonecutter rollout.
        // 1.21.1 is the first widely-adopted point release after 1.21
        // (Tricky Trials). 1.21.4 ("Bundles of Bravery") introduces
        // PALE_OAK_SAPLING / EYEBLOSSOM variants but NOT the 1.21.5 dry-grass
        // / bush / wildflowers symbols. 1.21.5 ("Spring to Life") introduces
        // several blocks the mod cares about (BambooShootBlock rename,
        // SHORT_DRY_GRASS, TALL_DRY_GRASS, BUSH, FIREFLY_BUSH, CACTUS_FLOWER,
        // WILDFLOWERS). 1.21.8 is the latest stable mid-series. 1.21.11 is
        // the final 1.21 series version.
        //
        // 1.21.4 needs its own build because the 1.21.5 source tree references
        // Blocks.SHORT_DRY_GRASS / BUSH / WILDFLOWERS / etc. — fields that do
        // not exist in 1.21.4's vanilla Blocks class. Running the 1.21.5 jar
        // on a 1.21.4 server would throw NoSuchFieldError on class load.
        // Stonecutter's `//? if >=1.21.5` guards strip those entries from the
        // 1.21.4 chiseled output while preserving the rest of the source tree.
        versions("1.21.1", "1.21.4", "1.21.5", "1.21.8", "1.21.11")

        // Active version when developers run `./gradlew build` / `runClient`
        // without first switching projects. Kept at 1.21.5 to minimize
        // disruption to the existing dev workflow.
        vcsVersion = "1.21.5"
    }
}

rootProject.name = "placeable-plants"
