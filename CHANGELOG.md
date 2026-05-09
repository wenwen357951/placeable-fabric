# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [1.3.0] - 2026-05-09

This release is a comprehensive overhaul. It rebuilds the worldgen guard,
switches the build to Stonecutter with five build targets, adds an automated
test suite, fills gaps in the plant catalogue covering all 12 1.21.x patch
versions, and ships CI/CD via GitHub Actions.

### Added

- **5-jar multi-version build** via Stonecutter covering all twelve 1.21.x
  patch versions: 1.21.1 (serves 1.21–1.21.3), 1.21.4 (1.21.4 only),
  1.21.5 (1.21.5 only), 1.21.8 (1.21.6–1.21.8), and 1.21.11
  (1.21.9–1.21.11). Single source tree, single `./gradlew chiseledBuild`
  invocation produces every jar. The dedicated 1.21.4 target exists because
  "Bundles of Bravery" lacks the 1.21.5 Spring-to-Life symbols (BUSH,
  FIREFLY_BUSH, SHORT_DRY_GRASS, TALL_DRY_GRASS, CACTUS_FLOWER,
  WILDFLOWERS) — running the 1.21.5 jar on 1.21.4 would throw
  `NoSuchFieldError` on class load.
- **New placeable plants in the catalogue**:
    - 1.21+ (every supported target): `POTATOES`, `CARROTS`.
    - 1.21.4+ (`//? if >=1.21.4`): `PALE_OAK_SAPLING`, `CLOSED_EYEBLOSSOM`,
      `OPEN_EYEBLOSSOM`.
    - 1.21.5+ (`//? if >=1.21.5`): `BUSH`, `FIREFLY_BUSH`, `LEAF_LITTER`,
      `WILDFLOWERS`, `CACTUS_FLOWER`, `SHORT_DRY_GRASS`, `TALL_DRY_GRASS`.
      Existing user configs gain the new entries default-on on first load
      (no more silent opt-out on upgrade).
- **World-guard architecture** centred on
  `Placeable.shouldBypass(world, pos)`:
    - `world instanceof World` separates worldgen `ChunkRegion` calls from
      runtime `ServerWorld` / `ClientWorld` calls.
    - A per-thread `ThreadLocal<Integer> NATURAL_TICK_DEPTH` separates
      vanilla `randomTick` / `scheduledTick` frames from player frames.
    - `AbstractBlockStateNaturalTickMixin` (NEW) brackets vanilla
      `randomTick` / `scheduledTick` with `@WrapMethod` + try/finally so an
      exception in vanilla code can't leak the depth counter.
    - `BoneMealItemMixin` (NEW) does the same bracket on
      `BoneMeal.useOnGround` and `useOnFertilizable` so flower / bush /
      grass spawn from bone meal stays vanilla-identical.
- **Automated test suite** (JUnit 5 Jupiter + Mockito 5 +
  `fabric-loader-junit`): per-target unit tests for `Placeable`,
  `PlaceableConfig`, `PlaceablePlants`, and the air-floor regression in
  `IsValidFloorTest`. The ThreadLocal natural-tick contract has explicit
  nesting, exception-safety, and cross-thread isolation tests.
- **CI/CD** via GitHub Actions:
    - `build.yml` runs `chiseledBuild` plus
      `:1.21.1:test :1.21.4:test :1.21.5:test :1.21.8:test :1.21.11:test`
      on every push and PR; uploads built jars as workflow artifacts.
    - `release.yml` triggers on `v*` tag push, fans out across the same
      five MC versions via a matrix, and publishes each jar to Modrinth,
      CurseForge, and GitHub Releases through `Kir-Antipov/mc-publish`
      (SHA-pinned).
- **New documentation**: `docs/VERSIONS.md` (per-version compatibility
  map, "Why a separate 1.21.4 build?" rationale, upgrade-new-MC checklist)
  and `docs/MIXIN_COVERAGE.md` (transitive vs dedicated mixin map).

### Changed

- **World-guard refactor**: every `canPlaceAt` mixin now routes through
  `Placeable.shouldBypass(WorldView, BlockPos)` as its first action,
  before any `setReturnValue(true)`. The previous per-mixin
  `ChunkStatus.FULL` check is retired in favour of the centralised
  `world instanceof World` + natural-tick gate.
- **Recipes** use the cross-version-compatible plain-string ingredient
  form. `tall_dry_grass.json` is gated via `fabric:load_conditions`
  (`fabric:registry_contains` against `minecraft:tall_dry_grass`) so it
  loads only on 1.21.5+ targets.
- **`isDisable` → `isDisabled`**: renamed for grammatical correctness;
  deprecated aliases removed.
- **Mixin name corrections**: `BambooSaplingBlockMixin` →
  `BambooShootBlockMixin` (matches 1.21.5's `BambooShootBlock` rename);
  `BigDripleafSteamBlockMixin` → `BigDripleafStemBlockMixin` (typo fix).
- **Mod no longer widens vanilla mushroom natural spread** — the refactor
  of `MushroomPlantBlockMixin` plus the `shouldBypass` gate in every
  `canPlaceAt` mixin together restore byte-identical mushroom spread
  targets relative to vanilla.
- **Air can no longer be a "valid floor"**, even with
  `placedWithoutTopRim=true`. Without this guard the permissive config
  short-circuited the floor check to `true` and the player could place
  plants in mid-air.
- **Config holder is cached** on first read (`Placeable.CONFIG_HOLDER`)
  so the `canPlaceAt` hot path is branch-free of AutoConfig lookups.
- **`PlaceablePlants.findBy(Block)`** moved from O(n) iteration to an
  O(1) `Map<Block, PlaceablePlants>` lookup.
- **`fabric.mod.json` dependency ranges** are now expanded by Stonecutter
  per chiseled subproject so each jar declares the correct
  `depends.minecraft` window and `cloth-config2` floor for its MC line:
  `>=1.21- <1.21.4` (1.21.1 jar), `>=1.21.4- <1.21.5` (1.21.4 jar),
  `>=1.21.5- <1.21.6` (1.21.5 jar), `>=1.21.6- <1.21.9` (1.21.8 jar),
  `>=1.21.9- <1.22` (1.21.11 jar).
- **`placeable.mixins.json`** template-expands a per-version
  `${leaf_litter_mixin_entry}` so `LeafLitterBlockMixin` is registered
  only on 1.21.5+ (its target class doesn't exist on older versions).

### Fixed

- Default-true behaviour preserved on config upgrade for newly-introduced
  plants — `PlaceableConfig.validatePostLoad()` fills missing keys with
  `true` and drops orphan keys on load.
- `CocoaBlockMixin` now consults the central worldgen guard (previously
  missing — natural cocoa spread could sneak past the mod).
- `CactusBlockMixin`'s "cactus stacked on cactus" short-circuit no longer
  bypasses the natural-tick guard.
- `MushroomPlantBlockMixin` no longer widens the spread gradient on
  relaxed-rule placements (combined with `shouldBypass`).
- Bone meal no longer widens flower / tall grass / bush spawn surfaces —
  `BoneMealItemMixin` brackets the call so
  `Fertilizable.findPosToSpreadTo`'s nested `canPlaceAt` calls hit the
  natural-tick branch and defer to vanilla.
- `Placeable.getConfig()` race-window semantics are documented; null
  reads are treated as "mod disabled" (default-deny).
- `:1.21.1:test` no longer fails with
  `FabricLoaderLauncherSessionListener could not be instantiated` — the
  Stonecutter-baked `fabric.mod.json` now declares a Cloth Config floor
  that matches the 15.x line shipped with 1.21.1.
- Recipes no longer use the 1.21.5-only ingredient-array shape on older
  targets.
- `LEAF_LITTER` is now a placeable plant on 1.21.5+; previously excluded
  as "decorative carpet" but that contradicted the mod's "all plants
  placeable anywhere" mission.

### Removed

- **Dead code**: `DryVegetationBlockMixin` — its target class never
  reached `canPlantOnTop` because `PlantBlockMixin` already short-circuits
  in the parent class.
- **Deprecated `isDisable` aliases**: the rename to `isDisabled` shipped
  one release with `@Deprecated` shims; this release removes them.

### Internal

- `Placeable.LOGGER` now uses `LoggerFactory.getLogger(Placeable.class)`
  for SLF4J-conventional logger naming.
- All `randomTick` mixin handlers carry the `placeable$` prefix to avoid
  colliding with other mods' mixin handler names on shared target classes.
- Stonecutter plugin 0.9.3 wired in `settings.gradle.kts` and
  `stonecutter.gradle.kts`. Per-version coordinates live in
  `versions/<mc>/gradle.properties`.

## [mc1.21-1.1.1-HOTFIX] - 2024-08-21

### Fixed

- Null pointer error while loading configuration file!

## [mc1.21-1.1.1] - 2024-08-21

### Added

- Added red/brown mushroom plants that can be placed anywhere.
- Added yaml file version control (for Dev)
- Support Minecraft 1.21.1

### Fixed

- Fixed missing crops potatoes and carrots in configuration.

## [mc1.21-1.1.0] - 2024-07-26

### Added

- Add Configuration functionality to control whether each plant can be placed anywhere.
- Add tall grass and large ferns to recipe.
- Add new plants Cactus can be placed anywhere.
- Add new plants Sugar Cane can be placed anywhere.
- Add new plants Small Dripleaf can be placed anywhere.

### Fixed

- Fixed an issue with bamboo growing on invalid ground when stacking two bamboos
- Fix an issue where mangrove propagules could grow on illegal blocks.
- Fix an issue where Nehter Wart could grow on illegal blocks.
- Fix an issue where Sweet Berry Bush could grow on illegal blocks.
- Fix an issue where Pitcher Pod could grow on illegal blocks.
- Fix an issue where Pumpkin and Melon Stem could grow on illegal blocks.
- Fix an issue where Mangrove Propagules could grow on illegal blocks.

### Change

- Improve gradle build
- Migrating build logic from Groovy to Kotlin.

## [mc1.21-1.0.4] - 2024-07-22

### Added

- Updated mod to support Minecraft 1.21.

### Changed

- Update [README.md](https://github.com/wenwen357951/placeable-fabric/blob/main/README.md) link and some information.

[unreleased]: https://github.com/wenwen357951/placeable-fabric/compare/mc1.21-1.1.2...HEAD

[mc1.21-1.1.1-HOTFIX]: https://github.com/wenwen357951/placeable-fabric/compare/mc1.21-1.1.1...mc1.21-1.1.2

[mc1.21-1.1.1]: https://github.com/wenwen357951/placeable-fabric/compare/mc1.21-1.1.0...mc1.21-1.1.1

[mc1.21-1.1.0]: https://github.com/wenwen357951/placeable-fabric/compare/mc1.21-1.0.4...mc1.21-1.1.0

[mc1.21-1.0.4]: https://github.com/wenwen357951/placeable-fabric/releases/tag/mc1.21-1.0.4
