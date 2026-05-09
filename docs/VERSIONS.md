# Minecraft Version Support Matrix

**Status of this document**: living reference. Edits should accompany every
addition or removal of a Stonecutter target version.

## Supported MC versions (first-class)

These are the versions exercised by `./gradlew chiseledBuild` and present in
`settings.gradle.kts`'s `versions(...)` declaration. Each builds and runs as
a tested artifact.

| MC version | Status      | Yarn build (current) | Notes                                                                                                                                                                                                                                                          |
|------------|-------------|----------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 1.21.1     | first-class | `1.21.1+build.3`     | First widely-adopted point release after 1.21 / Tricky Trials.                                                                                                                                                                                                 |
| 1.21.4     | first-class | `1.21.4+build.8`     | "Bundles of Bravery" — adds PALE_OAK_SAPLING, CLOSED_EYEBLOSSOM, OPEN_EYEBLOSSOM. Lacks 1.21.5+ symbols (BUSH, FIREFLY_BUSH, SHORT_DRY_GRASS, TALL_DRY_GRASS, CACTUS_FLOWER, WILDFLOWERS), so it requires its own Stonecutter target — see "Why a separate 1.21.4 build?" below. |
| 1.21.5     | first-class | `1.21.5+build.1`     | "Spring to Life" — adds BUSH, FIREFLY_BUSH, WILDFLOWERS, SHORT_DRY_GRASS, TALL_DRY_GRASS, CACTUS_FLOWER. Renames `BambooSaplingBlock` → `BambooShootBlock`. Default Stonecutter active version.                                                                |
| 1.21.8     | first-class | `1.21.8+build.1`     | Stable mid-series; no plant-API drift relative to 1.21.5.                                                                                                                                                                                                      |
| 1.21.11    | first-class | `1.21.11+build.5`    | Final 1.21 series version. "Mounts of Mayhem".                                                                                                                                                                                                                 |

## Per-version compatibility map

The 1.21 series spans 12 game versions (1.21, 1.21.1, …, 1.21.11). The mod
ships **5 jars** that together cover **all 12** by leveraging each game-drop's
internal patch line (Mojang's "drop X.Y" branches share the same plant block
API across patch releases within the drop) and adding a dedicated 1.21.4
build to handle the symbol gap between "Bundles of Bravery" and
"Spring to Life".

The mapping below is the authoritative source for the `game-versions:` field
in `.github/workflows/release.yml`'s mc-publish step and for the Modrinth /
CurseForge "supported versions" list per release entry.

| Built jar | Drop name           | Patch versions covered       | Modrinth / CF `game-versions` value | `depends.minecraft` (fabric.mod.json) |
|-----------|---------------------|------------------------------|-------------------------------------|---------------------------------------|
| 1.21.1    | Tricky Trials *     | 1.21, 1.21.1, 1.21.2, 1.21.3 | `1.21,1.21.1,1.21.2,1.21.3`         | `>=1.21- <1.21.4`                     |
| 1.21.4    | Bundles of Bravery  | 1.21.4                       | `1.21.4`                            | `>=1.21.4- <1.21.5`                   |
| 1.21.5    | Spring to Life      | 1.21.5                       | `1.21.5`                            | `>=1.21.5- <1.21.6`                   |
| 1.21.8    | Chase the Skies     | 1.21.6, 1.21.7, 1.21.8       | `1.21.6,1.21.7,1.21.8`              | `>=1.21.6- <1.21.9`                   |
| 1.21.11   | Mounts of Mayhem ** | 1.21.9, 1.21.10, 1.21.11     | `1.21.9,1.21.10,1.21.11`            | `>=1.21.9- <1.22`                     |

\* The 1.21.1 jar is bytecode-compatible across 1.21 → 1.21.3 because the
plant Block roster is unchanged between Tricky Trials and the pre-Bundles
1.21.2 / 1.21.3 patches. Bundles of Bravery (1.21.4) is the first patch to
introduce new plant blocks (PALE_OAK_SAPLING, CLOSED_EYEBLOSSOM,
OPEN_EYEBLOSSOM), which is why the 1.21.4 build target exists.

\*\* "Copper Age" (1.21.9 / 1.21.10) and "Mounts of Mayhem" (1.21.11) share
the same plant block roster per the Minecraft Wiki "Java Edition" patch notes.
No new placeable plant blocks were added across 1.21.9–1.21.11, so a single
1.21.11 jar serves all three.

### Why a separate 1.21.4 build?

The 1.21.5 ("Spring to Life") source tree references vanilla `Blocks` static
fields that DO NOT EXIST in 1.21.4's vanilla `Blocks` class:

- `Blocks.SHORT_DRY_GRASS`, `Blocks.TALL_DRY_GRASS`
- `Blocks.BUSH`, `Blocks.FIREFLY_BUSH`
- `Blocks.WILDFLOWERS`
- `Blocks.CACTUS_FLOWER`

If the 1.21.5 jar were declared as covering 1.21.4, Fabric Loader would
happily load the jar on 1.21.4 — and class initialisation of
`PlaceablePlants` would then throw `NoSuchFieldError: SHORT_DRY_GRASS` (or
one of the others) the moment any mixin called `Placeable.isDisabled` for
the first time. The mod would crash on every block-placement attempt.

The fix is structural: the 1.21.4 build target is its own Stonecutter
chiseled subproject. Its source tree is preprocessed with `//? if >=1.21.5`
guards stripped in PlaceablePlants.java, removing all six fields above while
preserving PALE_OAK_SAPLING / CLOSED_EYEBLOSSOM / OPEN_EYEBLOSSOM (which DO
exist in 1.21.4 — they shipped in Bundles of Bravery). The resulting jar's
`fabric.mod.json` declares `minecraft: >=1.21.4- <1.21.5`, so Loader will
refuse to load it on neighbouring patches and players see the correct file
on Modrinth / CurseForge.

### Boundary justification (per intermediate version)

| Patch version | Drop                     | New plant blocks vs. previous patch                  | Reason it maps to the listed jar                                                |
|---------------|--------------------------|------------------------------------------------------|---------------------------------------------------------------------------------|
| 1.21          | Tricky Trials            | (baseline of the 1.21 series)                        | Server-protocol-compatible with 1.21.1; same plant roster.                      |
| 1.21.2        | Bundles of Bravery (pre) | None of mod interest                                 | Stable plant API between 1.21.1 and 1.21.4.                                     |
| 1.21.3        | Bundles of Bravery (pre) | None of mod interest                                 | Same as 1.21.2.                                                                 |
| 1.21.4        | Bundles of Bravery       | PALE_OAK_SAPLING, CLOSED_EYEBLOSSOM, OPEN_EYEBLOSSOM | Has its own dedicated build target — see "Why a separate 1.21.4 build?" above.  |
| 1.21.6        | Chase the Skies          | None of mod interest                                 | "Happy Ghasts" — no plant additions per wiki.                                   |
| 1.21.7        | Chase the Skies (hotfix) | None                                                 | Hotfix release inside the same drop.                                            |
| 1.21.9        | Copper Age               | None of mod interest                                 | "Copper Age" — copper bulb, copper torches; no placeable plants added per wiki. |
| 1.21.10       | Copper Age (hotfix)      | None                                                 | Hotfix.                                                                         |

### Adding a new build target

If a future Mojang release introduces a new placeable plant block (or renames
an existing block class — see `BambooSaplingBlock` → `BambooShootBlock` in
1.21.5), the mod must add a NEW first-class Stonecutter target rather than
extend an existing jar's compat range. The procedure is:

1. Create `versions/<new-mc>/gradle.properties` (template: 1.21.5).
2. Add the version string to the `versions(...)` list in `settings.gradle.kts`.
3. Add a matrix entry to the `release.yml` `matrix.include` list, including
   the `game_versions:` comma-list of patch versions the new jar serves.
4. If the new MC version introduces a new plant or renames a class, follow
   the "Upgrade-new-MC-version checklist" below.

## Versions explicitly OUT OF SCOPE for this mod's lifecycle

| MC version | Status       | Reason                                                                                                                                          |
|------------|--------------|-------------------------------------------------------------------------------------------------------------------------------------------------|
| 26.1+      | OUT OF SCOPE | Fabric 26.1 is a fundamental break: Mojmap (no Yarn), Java 25, new Loom plugin ID, no compat layer with 1.21.x. Tracked as a separate future project lifecycle. |

## Upgrade-new-MC-version checklist

Use this when adding a new MC version (call it `<MC>`) to the first-class
set. The order matters — earlier steps unblock later ones.

### Step 1 — Stonecutter / build infra

1. Add `<MC>` to the `versions(...)` list in `settings.gradle.kts` inside
   `stonecutter { create(rootProject) { versions(...) } }`.
2. Create `versions/<MC>/gradle.properties` with at least:
   ```
   minecraft_version=<MC>
   yarn_mappings=<MC>+build.<N>
   loader_version=<latest>
   fabric_version=<latest matching <MC>>
   ```
   Use `versions/1.21.5/gradle.properties` as a template.
3. Run `./gradlew "<MC>:build"` once. Capture the cannot-find-symbol errors.

### Step 2 — Plant audit

4. Extract the merged Loom jar's `Blocks.class`:
   ```bash
   jar=$(ls C:/Users/<you>/.gradle/caches/fabric-loom/minecraftMaven/net/minecraft/minecraft-merged/<MC>-*/*.jar | head -1)
   unzip -p "$jar" net/minecraft/block/Blocks.class > /tmp/Blocks_<MC>.class
   javap -p /tmp/Blocks_<MC>.class | grep -iE "PLANT|FLOWER|GRASS|SAPLING|FERN|BUSH|MUSHROOM|CROP|WART|ROOTS|SPROUTS|PETALS|FUNGUS|BERRY|POD|WILDFLOWER|VINE|LEAVES|LICHEN|EYEBLOSSOM|AZALEA|PROPAGULE" | sort -u
   ```
5. Cross-reference the output against `PlaceablePlants.java`. For each new vanilla
   plant block:
    - If it fits the mod's "place a plant on top of any block" mission, add an
      enum entry under `//? if >=<MC>`.
    - If it does NOT (carpet / hanging / underwater / ceiling-attached), add an
      "intentionally excluded" comment in the enum's javadoc with the rationale.

### Step 3 — Code-level conditioning

6. For every cannot-find-symbol from Step 1 step 3, wrap the offending source
   line with `//? if >=<MC>` (introducing the symbol) or `//? if <<MC>` (removing
   it on older versions). Stonecutter strips the line for non-matching targets.
7. For renamed classes (e.g., `BambooSaplingBlock` → `BambooShootBlock`),
   wrap both the import statement AND the `@Mixin(...)` target with `//? if`
   else-blocks.
8. For breaking method-signature changes within a class, wrap the affected
   method body with `//? if`. If the change is large, prefer adding two source
   files and conditioning the import — the Stonecutter docs suggest this
   pattern for any change >5 lines.

### Step 4 — Verification

9. Run `./gradlew chiseledBuild` and confirm all (now five+) versions PASS.
   If any FAIL, fix before merging.
10. Manually launch each version's `runClient` and place at least one of
    each plant category (sapling / flower / crop / sugar cane / cactus /
    mushroom) on a slab and on a leaves block.
11. Run the mushroom non-regression smoke test: place a brown mushroom on a
    stone slab in a dark cave; confirm it does NOT spread to surrounding
    slabs after random-tick.

### Step 5 — Documentation

12. Update this file's "Supported MC versions" table.
13. Bump `mod_version` in `gradle.properties` per CHANGELOG conventions.
14. Update `CHANGELOG.md` with the new MC target.

### Step 6 — Release

15. Tag the release: `git tag v<mod_version>`.
16. Push the tag; CI (`mc-publish` workflow) handles Modrinth, CurseForge, and
    GitHub Releases.

## Reference: 26.x / Mojmap migration plan (deferred)

26.x represents a fundamental ecosystem break. The 1.21 series mod will NOT
receive a 26.x port directly — instead a separate parallel project lifecycle
should:

1. Branch the repository into `26.x-mojmap`.
2. Replace Yarn `net.minecraft.*` references with Mojmap equivalents.
3. Migrate `fabric-loom` plugin ID to `net.fabricmc.fabric-loom`.
4. Replace `modImplementation` with `implementation` and `remapJar` with `jar`.
5. Update Java target from 21 to 25.
6. Adapt to renamed Fabric API symbols (e.g., `ItemGroupEvents` →
   `CreativeModeTabEvents`).

This is deferred. Re-evaluate when the player base for 1.21.x drops below
maintenance threshold.
