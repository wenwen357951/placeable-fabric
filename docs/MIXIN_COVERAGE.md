# Mixin Coverage Map

**Audience**: maintainers adding placeable-plant support, debugging
unexpected vanilla interactions, or porting to a new MC version.

**Purpose**: document, per vanilla plant class, whether `canPlaceAt` is
covered transitively via the shared `PlantBlock` super-call chain or
requires a dedicated mixin, and what each dedicated mixin handles beyond
`canPlaceAt`.

---

## How the coverage chain works

Every vanilla plant block's `canPlaceAt(BlockState, WorldView, BlockPos)` is
the gate Mojang checks before allowing a block at a given position. The
mod's strategy:

1. The shared `PlantBlockMixin` injects at HEAD of `PlantBlock.canPlaceAt`
   and short-circuits with `setReturnValue(true)` whenever:
    - The mod is enabled (`Placeable.shouldBypass` returns false), AND
    - The plant entry is on (`Placeable.isDisabled(state)` returns false), AND
    - The floor under the plant qualifies (`Placeable.isValidFloor(world, pos)`).
2. Subclasses that **call `super.canPlaceAt(...)`** inside their override
   inherit the relaxed rule transitively — no per-subclass mixin needed.
3. Subclasses that **fully override** `canPlaceAt` without calling super
   must have their own dedicated mixin.

---

## Transitive coverage via PlantBlockMixin

These vanilla classes inherit the relaxed-placement rule from
`PlantBlockMixin` because their own `canPlaceAt` either does not exist or
ends with `super.canPlaceAt(...)`:

| Vanilla class                     | Mechanism                                                                     | Plants affected (representative)                                       |
|-----------------------------------|-------------------------------------------------------------------------------|------------------------------------------------------------------------|
| `PlantBlock`                      | direct mixin target.                                                          | DEAD_BUSH, NETHER_SPROUTS, ROOTS, FERN, FLOWERS, etc.                  |
| `TallPlantBlock`                  | LOWER half does `invokespecial PlantBlock.canPlaceAt`.                        | TALL_GRASS, LARGE_FERN, SUNFLOWER, LILAC, ROSE_BUSH, PEONY.            |
| `FlowerBlock`                     | extends `PlantBlock`, no override.                                            | All small flowers, EYEBLOSSOM (closed/open), TORCHFLOWER, WITHER_ROSE. |
| `TallFlowerBlock`                 | extends `TallPlantBlock`.                                                     | LILAC, ROSE_BUSH, PEONY (also covered above).                          |
| `BushBlock` (1.21.5+)             | extends `PlantBlock`, no override.                                            | BUSH, FIREFLY_BUSH.                                                    |
| `ShortPlantBlock`                 | extends `PlantBlock`, no override.                                            | SHORT_GRASS, FERN, SHORT_DRY_GRASS (1.21.5+).                          |
| `TallDryGrassBlock` (1.21.5+)     | extends `TallPlantBlock`.                                                     | TALL_DRY_GRASS.                                                        |
| `CarpetBlock` PINK_PETALS variant | PINK_PETALS extends `PlantBlock` per yarn.                                    | PINK_PETALS, WILDFLOWERS (1.21.5+).                                    |
| `PropaguleBlock` non-hanging      | calls `invokespecial SaplingBlock.canPlaceAt` → `PlantBlock.canPlaceAt`.      | MANGROVE_PROPAGULE (planted form).                                     |
| `PitcherCropBlock` lower-half     | guards on `hasEnoughLightAt`, then `invokespecial TallPlantBlock.canPlaceAt`. | PITCHER_CROP (lower half only).                                        |
| `PaleOakSaplingBlock` (1.21.4+)   | extends `SaplingBlock` → `PlantBlock`.                                        | PALE_OAK_SAPLING.                                                      |
| `CactusFlowerBlock` (1.21.5+)     | extends `PlantBlock`.                                                         | CACTUS_FLOWER.                                                         |

Implication: the unified `PlantBlockMixin` is the single biggest lever in
the mod. Adding a new flower / small grass block in a future MC release
usually means **only** adding an entry to `PlaceablePlants.java` — no new
mixin file.

---

## Dedicated mixins (vanilla classes that fully override canPlaceAt)

| Vanilla class                                                 | Dedicated mixin                       | Beyond `canPlaceAt`                                                                                                                                                                                                                                                                                                                                                                                       |
|---------------------------------------------------------------|---------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `MushroomPlantBlock`                                          | `MushroomPlantBlockMixin`             | Defers natural spread to vanilla via `Placeable.shouldBypass` — does NOT widen mushroom random-walk in dim caves. Light-based gating remains vanilla.                                                                                                                                                                                                                                                     |
| `CactusBlock`                                                 | `CactusBlockMixin`                    | injects `randomTick` to prevent stack-growth on non-sand floors so a player-placed cactus on cobblestone does not propagate into invalid stacks.                                                                                                                                                                                                                                                          |
| `BambooBlock`                                                 | `BambooBlockMixin`                    | overrides `getPlacementState` so player placement on a non-bamboo floor still produces a valid state; deferred growth gating mirrors vanilla.                                                                                                                                                                                                                                                             |
| `BambooShootBlock` (1.21.5+) / `BambooSaplingBlock` (≤1.21.4) | `BambooShootBlockMixin`               | initial-stage placement; class rename is wrapped in `//? if` per Stonecutter setup.                                                                                                                                                                                                                                                                                                                       |
| `SugarCaneBlock`                                              | `SugarCaneBlockMixin`                 | mirrors cactus pattern — relaxes placement, does not widen vanilla growth onto non-water-adjacent floors via `randomTick` guard.                                                                                                                                                                                                                                                                          |
| `CocoaBlock`                                                  | `CocoaBlockMixin`                     | side-attached to jungle-log shape; mixin intentionally narrow — relaxes only the `canPlaceAt` half, NOT the attach-target half.                                                                                                                                                                                                                                                                           |
| `BigDripleafBlock`                                            | `BigDripleafBlockMixin`               | dripleaf has unique downward-attachment quirks; relaxed top-rim rule applies to the head only.                                                                                                                                                                                                                                                                                                            |
| `BigDripleafStemBlock`                                        | `BigDripleafStemBlockMixin`           | stem segment placement; preserves vanilla "must be under a head" invariant.                                                                                                                                                                                                                                                                                                                               |
| `SmallDripleafBlock`                                          | `SmallDripleafBlockMixin`             | clay/water-adjacency override; vanilla biome restriction lifted, top-rim still enforced.                                                                                                                                                                                                                                                                                                                  |
| `CropBlock`                                                   | `CropBlockMixin`                      | covers WHEAT, CARROTS, POTATOES, BEETROOTS, TORCHFLOWER_CROP. The vanilla `canPlaceAt` gates on `hasEnoughLightAt` before super-calling, so light gating remains vanilla — the mixin only restricts random-tick growth on non-farmland.                                                                                                                                                                   |
| `StemBlock`                                                   | `StemBlockMixin`                      | covers PUMPKIN_STEM, MELON_STEM. Attached vs non-attached state handled by vanilla; mixin restricts random-tick growth to farmland.                                                                                                                                                                                                                                                                       |
| `PitcherCropBlock`                                            | `PitcherCropBlockMixin`               | upper-half handling; lower half is covered transitively via TallPlantBlock chain.                                                                                                                                                                                                                                                                                                                         |
| `NetherWartBlock`                                             | `NetherWartBlockMixin`                | restricts random-tick growth to soul sand.                                                                                                                                                                                                                                                                                                                                                                |
| `SaplingBlock`                                                | `SaplingBlockMixin`                   | sapling-specific dirt requirement; restricts random-tick growth so saplings on cobblestone never grow into trees.                                                                                                                                                                                                                                                                                         |
| `PropaguleBlock`                                              | `PropaguleBlockMixin`                 | hanging-state branch (non-hanging falls through to SaplingBlock chain).                                                                                                                                                                                                                                                                                                                                   |
| `SweetBerryBushBlock`                                         | `SweetBerryBushBlockMixin`            | restricts random-tick growth to dirt / farmland.                                                                                                                                                                                                                                                                                                                                                          |
| `LeafLitterBlock` (1.21.5+)                                   | `LeafLitterBlockMixin`                | LeafLitterBlock's vanilla `canPlaceAt` requires a fully-solid top face (rejects slabs / stairs / leaves / dirt-path); the mixin relaxes that to the standard `isValidFloor` rule. The mixin entry is template-elided from `placeable.mixins.json` on the 1.21.1 / 1.21.4 jars because the target class doesn't exist there — see `build.gradle.kts` `${leaf_litter_mixin_entry}` substitution.            |

### Cross-cutting mixin

| Mixin                                | Purpose                                                                                                                                                                                                                                                       |
|--------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `AbstractBlockStateNaturalTickMixin` | Brackets `randomTick` and `scheduledTick` calls with `enterNaturalTick` / `exitNaturalTick` so `Placeable.shouldBypass` returns true while vanilla natural-tick frames execute. Required so vanilla natural ecology spread is never widened by the mod.       |

### Item-level mixin

This mixin targets a vanilla `Item`, not a `Block`, so it lives outside the
"block plant" coverage table above. It exists because some items invoke
`BlockState.canPlaceAt` directly while running player-triggered but
vanilla-internal evolution loops, and the natural-tick gate must be set for
the duration of those calls.

| Mixin               | Vanilla target                                               | Purpose                                                                                                                                                                                                                                                                                                                                                                                                                                            |
|---------------------|--------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `BoneMealItemMixin` | `BoneMealItem.useOnGround`, `BoneMealItem.useOnFertilizable` | Brackets each entry point with `enterNaturalTick` / `exitNaturalTick` so the `BlockState.canPlaceAt` calls nested inside vanilla's spawn-surface scan and `Fertilizable.findPosToSpreadTo` (used by `BushBlock` / `FireflyBushBlock`) hit the natural-tick branch of `Placeable.shouldBypass` and produce vanilla-identical bone-meal output. Implemented with `@WrapMethod` for try/finally semantics. Server-side mixin (bone meal is server-authoritative). |

---

## Adding support for a new plant

Decision tree for "should this plant get a dedicated mixin?":

1. Inspect the vanilla class. Run:
   ```bash
   javap -p -c net/minecraft/block/<TheBlock>.class | grep -A 5 canPlaceAt
   ```
2. If the class has no `canPlaceAt` override, OR the override ends with
   `invokespecial <ParentClass>.canPlaceAt(...)`, you are covered transitively.
   **Do nothing in `mixin/`** — only add the entry to `PlaceablePlants.java`.
3. If the override is self-contained (no super-call), a new dedicated mixin
   file is needed:
    - Create `<ClassName>Mixin.java` in
      `src/main/java/com/wennest/placeable/mixin/`.
    - Follow the standard shape:
      ```java
      @Inject(method = "canPlaceAt", at = @At("HEAD"), cancellable = true)
      private void onCanPlaceAt(BlockState state, WorldView world, BlockPos pos,
                                CallbackInfoReturnable<Boolean> cir) {
          if (Placeable.shouldBypass(world, pos)) return;
          if (Placeable.isDisabled(state)) return;
          if (Placeable.isValidFloor(world, pos)) {
              cir.setReturnValue(true);
          }
      }
      ```
    - Register the mixin in `src/main/resources/placeable.mixins.json`'s
      `"mixins"` array.
    - Add the enum entry to `PlaceablePlants.java`.
4. If the plant has a "natural growth" behavior that is not desirable on
   non-vanilla floors (cactus stacking on cobblestone, sugar cane growing
   on non-water-adjacent floors), additionally inject `randomTick` to
   short-circuit when the floor isn't vanilla-valid. See
   `CactusBlockMixin.randomTick` for the reference pattern.

## Maintenance discipline

- The set of dedicated mixins should grow only by demand from new vanilla
  classes, never by duplicating logic that PlantBlockMixin already covers.
- When a new MC release renames or splits a vanilla class (e.g.,
  `BambooSaplingBlock` → `BambooShootBlock` in 1.21.5), wrap the import and
  `@Mixin(...)` target with `//? if` per Stonecutter — do NOT branch the
  source tree.
- When deleting a dedicated mixin, audit the `mixins` array in
  `placeable.mixins.json` and the `PlaceablePlants` enum — both must stay
  consistent.
