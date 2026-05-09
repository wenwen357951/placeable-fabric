<p align="center">
    <br>
    <img src="https://raw.githubusercontent.com/wenwen357951/placeable-fabric/main/src/main/resources/assets/placeable/icon.png" alt="Placeable Plants Logo" width="256">
    <br>
</p>

<h4 align="center">Source code of the Placeable Plants fabric mod.</h4>
<p align="center">
    <a href="https://www.codefactor.io/repository/github/wenwen357951/placeable-fabric">
        <img src="https://www.codefactor.io/repository/github/wenwen357951/placeable-fabric/badge?style=for-the-badge" alt="CodeFactor Score">
    </a>
    <a href="https://discord.gg/DdaCWYqmZt">
        <img src="https://img.shields.io/discord/1141595063567273995?style=for-the-badge" alt="Discord chat" />
    </a>
    <a href="https://modrinth.com/mod/placeable-plants">
        <img src="https://img.shields.io/modrinth/dt/placeable-plants?style=for-the-badge" alt="Modrinth downloads" />
    </a>
    <img src="https://img.shields.io/github/license/wenwen357951/placeable-fabric?style=for-the-badge" alt="GitHub License" />
</p>

## 🪴 What is it?

<center>
    <p align="center"><b>This Fabric Mod allows you to place plants on almost all blocks.</b></p>
    <img src="https://cdn.modrinth.com/data/o3wjLmDn/images/3eb7e86e4f0d0077abef1214e6b7cda8a49fe1d7.png" alt="Grass placed on weird blocks">
    <p align="center"><b>Fill your world with greenery!</b></p>
    <img src="https://cdn.modrinth.com/data/cached_images/4a3d778f72ba04dcd8f00d58563dbbd4a9680d2a.jpeg" alt="Grass on a cobblestone street">
</center>

## 🎉 Feature

- This Mod allows you to place plants on any flat surface. To do that, just right-click on a flat surface.
- Added recipes for Tall Dry Grass, Tall Grass and Large Fern

<p align="center">
    <img src="https://raw.githubusercontent.com/wenwen357951/placeable-fabric/refs/heads/docs/docs/img/recipe-tall-dry-grass.png" alt="The Tall Dry Grass Recipe" width="30%">
    <img src="https://raw.githubusercontent.com/wenwen357951/placeable-fabric/refs/heads/docs/docs/img/recipe-tall-grass.png" alt="The Tall Grass Recipe" width="30%">
    <img src="https://raw.githubusercontent.com/wenwen357951/placeable-fabric/refs/heads/docs/docs/img/recipe-large-fern.png" alt="The Large Fern Recipe" width="30%">
</p>

## 🎍 How to use?

When you right-click on a floor block while holding a plant, the plant is placed.

The supported plant catalogue (full list — `(MC X.Y+)` markers indicate the first
Minecraft version the entry is available on):

- **Saplings**: Oak, Spruce, Birch, Jungle, Acacia, Dark Oak, Cherry, Mangrove Propagule, Pale Oak (1.21.4+), Azalea,
  Flowering Azalea.
- **Flowers**: Dandelion, Poppy, Blue Orchid, Allium, Azure Bluet, Red / Orange / White / Pink Tulip, Oxeye Daisy,
  Cornflower, Lily of the Valley, Wither Rose, Torchflower, Closed Eyeblossom (1.21.4+), Open Eyeblossom (1.21.4+).
- **Tall flowers**: Sunflower, Lilac, Rose Bush, Peony, Pitcher Plant.
- **Grass-family**: Short Grass, Tall Grass, Short Dry Grass (1.21.5+), Tall Dry Grass (1.21.5+), Bush (1.21.5+),
  Firefly Bush (1.21.5+), Wildflowers (1.21.5+), Pink Petals, Leaf Litter (1.21.5+).
- **Ferns**: Fern, Large Fern.
- **Mushrooms / fungi**: Brown Mushroom, Red Mushroom, Crimson Fungus, Warped Fungus.
- **Crops**: Wheat, Beetroot, Potatoes, Carrots, Pumpkin Stem, Melon Stem, Torchflower Crop, Pitcher Crop, Cocoa.
- **Cacti / canes / bamboo**: Cactus, Cactus Flower (1.21.5+), Sugar Cane, Bamboo.
- **Berries / dead vegetation**: Sweet Berry Bush, Dead Bush.
- **Dripleaves / water plants**: Big Dripleaf, Small Dripleaf, Lily Pad.
- **Nether**: Nether Wart, Nether Sprouts, Crimson Roots, Warped Roots.

For the canonical, version-aware mapping (block → mixin → plant) see
[`docs/MIXIN_COVERAGE.md`](docs/MIXIN_COVERAGE.md), the `PlaceablePlants` enum in
[`src/main/java/com/wennest/placeable/PlaceablePlants.java`](src/main/java/com/wennest/placeable/PlaceablePlants.java),
and [`docs/VERSIONS.md`](docs/VERSIONS.md) for the per-version compatibility map.

## 📦 How to install?

1. Download the Mod
2. Navigate to your `mods` folder
3. Put the downloaded `.jar` file ([Placeable Plants]) into this folder

## 🛠️ Configuration

The mod allows you to control whether various plants can be placed anywhere (default: all enabled).

The primary way to configure this is through the mod menu's settings panel.

<img src="https://raw.githubusercontent.com/wenwen357951/placeable-fabric/refs/heads/docs/docs/img/placeable-config-setting.png" alt="Grass placed on weird blocks">

For advanced users, the `placeable.json` configuration file can also be directly modified as a secondary method. The
actual file is plain JSON (no comments) — the snippet below is the real shape, abbreviated:

```json
{
    "enable": true,
    "placedWithoutTopRim": false,
    "allowPlaceablePlants": {
        "OAK_SAPLING": true,
        "SPRUCE_SAPLING": true,
        "BIRCH_SAPLING": true,
        "JUNGLE_SAPLING": true,
        "ACACIA_SAPLING": true,
        "DARK_OAK_SAPLING": true,
        "MANGROVE_PROPAGULE": true,
        "CHERRY_SAPLING": true,
        "PALE_OAK_SAPLING": true,
        "AZALEA": true,
        "FLOWERING_AZALEA": true,
        "BROWN_MUSHROOM": true,
        "RED_MUSHROOM": true,
        "WHEAT": true,
        "POTATOES": true,
        "CARROTS": true,
        "BEETROOT": true,
        "CACTUS": true,
        "SUGAR_CANE": true,
        "BAMBOO": true,
        "NETHER_SPROUTS": true,
        "NETHER_WART": true,
        "LILY_PAD": true
    }
}
```

`enable: false` disables the mod globally; `placedWithoutTopRim: true` lets
plants drop the "floor must have a top rim" requirement (air is always
rejected). Any plant in the `allowPlaceablePlants` map can be flipped to
`false` to opt that single plant back to vanilla placement rules. Plants
newly introduced in a later mod release default to `true` on first load;
orphan keys (from a downgrade) are dropped.

## 🧩 Supported Minecraft versions

Placeable Plants 1.2.0 ships **5 jars** from a single source tree (managed
by Stonecutter) and together they cover **all 12** patch versions in the
1.21 family.

| Built jar | Drop name          | Patch versions covered       |
|-----------|--------------------|------------------------------|
| 1.21.1    | Tricky Trials      | 1.21, 1.21.1, 1.21.2, 1.21.3 |
| 1.21.4    | Bundles of Bravery | 1.21.4                       |
| 1.21.5    | Spring to Life     | 1.21.5                       |
| 1.21.8    | Chase the Skies    | 1.21.6, 1.21.7, 1.21.8       |
| 1.21.11   | Mounts of Mayhem   | 1.21.9, 1.21.10, 1.21.11     |

Each jar declares a tight `depends.minecraft` range so Fabric Loader
automatically refuses to load it on the wrong patch. See
[`docs/VERSIONS.md`](docs/VERSIONS.md) for the full per-version dependency
matrix, the rationale for the dedicated 1.21.4 build target, and the
future Mojmap (26.x) migration plan.

## 📌 Credits

[Bisumto] the original creator of this feature.


[Placeable Plants]: https://modrinth.com/mod/placeable-plants

[Bisumto]: https://github.com/BisUmTo/placeable
