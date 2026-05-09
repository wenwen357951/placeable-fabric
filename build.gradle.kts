// Central build script — evaluated once per Stonecutter chiseled subproject.
//
// Because `settings.gradle.kts` declares `centralScript = "build.gradle.kts"`,
// every `versions/<mc>` Stonecutter subproject reuses this exact file. Per-version
// inputs (Minecraft version, Yarn build, Fabric API, Cloth Config, ModMenu) are
// supplied via `versions/<mc>/gradle.properties` and read here as plain Gradle
// project properties. The shared root `gradle.properties` only carries truly
// version-agnostic values (mod group, archives base name, JVM args).
//
// The source tree stays unified; `//? if` comments in Java/JSON files are the
// only place a per-version fork is allowed. This script does NOT branch on
// version — it just plugs in the correct dependency coordinates for whatever
// version Stonecutter has activated.

plugins {
    alias(libs.plugins.fabric.loom)
}

// Mod identity is shared across all targets.
group = project.findProperty("maven_group")!!
// Mod version is composed of the human-readable `mod_version` plus the active
// Minecraft version, so the produced jar names disambiguate cleanly under
// `versions/<mc>/build/libs/` (and the chiseledBuild aggregation step).
version = "${project.findProperty("mod_version")}+${project.findProperty("minecraft")}"

repositories {
    // shedaniel — Cloth Config.
    maven("https://maven.shedaniel.me/")
    // Terraformers — ModMenu.
    maven("https://maven.terraformersmc.com/releases/")
}

dependencies {
    /* ---- Minecraft & Fabric core ---- */
    // Coordinates resolved entirely from per-version `gradle.properties` so the
    // same script handles 1.21.1 through 1.21.11 without modification.
    minecraft("com.mojang:minecraft:${property("minecraft")}")
    mappings("net.fabricmc:yarn:${property("yarn_mappings")}:v2")
    modImplementation("net.fabricmc:fabric-loader:${property("fabric_loader")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${property("fabric_api")}")

    /* ---- Third-party mod APIs ---- */
    // Cloth Config — config GUI + AutoConfig serializer (required at runtime).
    modApi("me.shedaniel.cloth:cloth-config-fabric:${property("cloth_config")}") {
        // Avoid pulling in a transitive ModMenu pin that fights ours.
        exclude(group = "com.terraformersmc")
    }
    // ModMenu — surfaces the config screen in the mods list.
    modApi("com.terraformersmc:modmenu:${property("modmenu")}")

    /* ---- Build-time / non-versioned libraries ----
     * Gson and Lombok do not vary across MC versions, so they continue to come
     * from the version catalog. */
    implementation(libs.gson)
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    /* ---- MixinExtras (compileOnly) ----
     * MixinExtras supplies @WrapMethod / Operation, used by
     * AbstractBlockStateNaturalTickMixin and BoneMealItemMixin to bracket
     * vanilla calls with try/finally semantics. Loom + Fabric Loader ship
     * MixinExtras at runtime, so it's only needed on the compile classpath —
     * declaring it as a regular implementation dep would risk shadowing the
     * runtime version. */
    compileOnly("io.github.llamalad7:mixinextras-fabric:0.5.0")

    /* ---- Test-only dependencies ----
     * JUnit 5 Jupiter + Mockito form the unit-test stack. Pure-logic helpers
     * in `Placeable`, `PlaceableConfig`, and `PlaceablePlants` are covered
     * here, as are the ThreadLocal natural-tick gate's nesting and
     * exception-safety contracts. These dependencies are kept out of the
     * production jar (testImplementation only) to avoid bloating the
     * redistributable. */
    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    // Gradle 9 requires an explicit launcher dep on the test runtime classpath
    // (older Gradle versions injected it implicitly).
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.mockito:mockito-core:5.11.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.11.0")

    /* ---- fabric-loader-junit ----
     * Fabric Loader's official JUnit 5 integration. When present on the test
     * classpath, Loom (>= 1.5) reroutes `./gradlew test` through Loader's dev
     * launcher: Yarn-mapped Minecraft classes are access-widened on the test
     * classpath, the registries are bootstrapped, and `Blocks.*` static fields
     * become safely usable from JUnit `@Test` methods.
     *
     * Without this artifact, plain `./gradlew test` cannot construct
     * `Blocks.OAK_SAPLING` because `SimpleRegistry.add` is package-private and
     * Loom's runtime widening is only applied to `runClient` / `runServer` —
     * unit tests would skip via `Assumptions.assumeTrue(...)` and never
     * exercise the `PlaceablePlants` enum / `PlaceableConfig` real paths.
     *
     * Pinned to the same version as `fabric-loader` to avoid classpath drift;
     * the Maven coordinate exists for every loader version targeted here. */
    testImplementation("net.fabricmc:fabric-loader-junit:${property("fabric_loader")}")
}

java {
    // All currently-targeted MC versions (1.21.1 – 1.21.11) use Java 21.
    // Java 25 is reserved for the future 26.x lifecycle phase.
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

base {
    archivesName = project.property("archives_base_name").toString()
}

tasks {
    processResources {
        // Expose mod_version (without the +mc suffix) and the resolved MC version
        // into fabric.mod.json so the manifest reports versions accurately at
        // runtime.
        //
        // Also expose `cloth_config_dep` — the minimum Cloth Config version this
        // build accepts at runtime. Defaults to the resolved `cloth_config`
        // coordinate so each chiseled subproject naturally produces a manifest
        // whose `cloth-config2` floor matches the actually-shipped Cloth Config
        // jar (1.21.1 ships Cloth 15.x, 1.21.5 ships 18.x, etc.). Without this,
        // the single fabric.mod.json template baked a 1.21.5-era floor
        // (>=18.0.145) into every build, causing fabric-loader-junit to refuse
        // loading the mod under 1.21.1 (which only has Cloth 15.x available).
        val modVersion = project.findProperty("mod_version").toString()
        val minecraft = project.findProperty("minecraft").toString()
        val clothConfigDep = (project.findProperty("cloth_config_dep")
            ?: project.findProperty("cloth_config")
            ?: "0").toString()
        // Per-build Minecraft dependency range — declared in
        // versions/<mc>/gradle.properties so each Stonecutter target advertises
        // the patch-version window its bytecode is compatible with. See
        // docs/VERSIONS.md "Per-version compatibility map" for the canonical
        // mapping (e.g., the 1.21.5 build declares `>=1.21.4- <1.21.6`).
        //
        // Fall back to `>=<minecraft>` when the property is missing so legacy
        // single-version invocations and ad-hoc local experiments still produce
        // a valid manifest; in CI the property is always set.
        val minecraftDep = (project.findProperty("minecraft_dep")
            ?: ">=$minecraft").toString()
        // LeafLitterBlockMixin only exists on 1.21.5+ because its target class
        // (net.minecraft.block.LeafLitterBlock) was added in "Spring to Life".
        // On older builds the mixin's class body is stripped by Stonecutter to
        // an empty class (no @Mixin annotation), and the Mixin loader would
        // fail-fast if the empty class were still listed in mixins.json. Plain
        // JSON does not support `//?` comments, so we can't gate the entry
        // inside the JSON file with Stonecutter's preprocessor; instead a
        // `${leaf_litter_mixin_entry}` placeholder is template-replaced at
        // processResources time based on the active MC version. The substituted
        // value is either the JSON entry plus its leading comma+newline, or the
        // empty string (eliding the entry entirely on older builds).
        val leafLitterMixinEntry = if (stonecutter.current.parsed matches ">=1.21.5")
            ",\n    \"LeafLitterBlockMixin\""
        else
            ""
        // Recipe ingredient codec divergence between 1.21.3 and 1.21.4:
        //   * 1.21 / 1.21.1 / 1.21.2 / 1.21.3 — the `key.<symbol>` value MUST
        //     be the object form `{"item": "<id>"}`. A plain string is
        //     rejected with "Not a JSON object: \"minecraft:fern\"".
        //   * 1.21.4 / 1.21.5 / 1.21.8 / 1.21.11 — the same field MUST be a
        //     plain string `"<id>"` (or array of strings). The legacy object
        //     form is rejected with "Input does not contain a key
        //     [fabric:type]" because Fabric's tagged-ingredient codec is
        //     tried first and the bare object lacks the discriminator.
        // No single literal ingredient form satisfies both eras, so the
        // per-version form is template-substituted into the recipe JSONs at
        // processResources time. Recipe templates use `${fern_ingredient}` /
        // etc. as the raw value of `"#"`, so the substitution must include
        // the JSON quoting/braces (NOT just the id).
        val ingredientForm: (String) -> String =
            if (stonecutter.current.parsed matches "<1.21.4")
                { id -> "{ \"item\": \"$id\" }" }
            else
                { id -> "\"$id\"" }
        val fernIngredient = ingredientForm("minecraft:fern")
        val shortGrassIngredient = ingredientForm("minecraft:short_grass")
        val shortDryGrassIngredient = ingredientForm("minecraft:short_dry_grass")
        inputs.property("version", modVersion)
        inputs.property("minecraft", minecraft)
        inputs.property("cloth_config_dep", clothConfigDep)
        inputs.property("minecraft_dep", minecraftDep)
        inputs.property("leaf_litter_mixin_entry", leafLitterMixinEntry)
        inputs.property("fern_ingredient", fernIngredient)
        inputs.property("short_grass_ingredient", shortGrassIngredient)
        inputs.property("short_dry_grass_ingredient", shortDryGrassIngredient)
        filesMatching("fabric.mod.json") {
            expand(
                mapOf(
                    "version" to modVersion,
                    "minecraft" to minecraft,
                    "cloth_config_dep" to clothConfigDep,
                    "minecraft_dep" to minecraftDep,
                ),
            )
        }
        // Separate filesMatching so the mixins.json substitution doesn't try
        // to expand `${minecraft_dep}` etc. (which aren't present in that file
        // and would either leak into the output or — worse — error out if a
        // future hand-edit dropped a literal `$` into the template). Each
        // file lists exactly the placeholders it actually uses.
        filesMatching("placeable.mixins.json") {
            expand(
                mapOf(
                    "leaf_litter_mixin_entry" to leafLitterMixinEntry,
                ),
            )
        }
        // Recipe JSONs — only the three shaped recipes that consume a vanilla
        // item ingredient need the codec-compat substitution.
        filesMatching("data/placeable/recipe/*.json") {
            expand(
                mapOf(
                    "fern_ingredient" to fernIngredient,
                    "short_grass_ingredient" to shortGrassIngredient,
                    "short_dry_grass_ingredient" to shortDryGrassIngredient,
                ),
            )
        }
    }

    compileJava {
        options.encoding = "UTF-8"
    }

    jar {
        from("LICENSE") {
            rename { "${it}_${project.base.archivesName.get()}" }
        }
    }

    // Wire the JUnit 5 Jupiter test engine. Required because Gradle's default
    // test runner is JUnit 4; without `useJUnitPlatform()` the @Test
    // annotations from `org.junit.jupiter.api` are silently ignored (zero
    // tests executed).
    test {
        useJUnitPlatform()
    }
}
