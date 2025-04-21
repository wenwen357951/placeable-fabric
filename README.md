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
    <img src="https://raw.githubusercontent.com/wenwen357951/placeable-fabric/refs/heads/main/docs/img/recipe-tall-dry-grass.png" alt="The Tall Dry Grass Recipe" width="25%">
    <img src="https://raw.githubusercontent.com/wenwen357951/placeable-fabric/refs/heads/main/docs/img/recipe-tall-grass.png" alt="The Tall Grass Recipe" width="25%">
    <img src="https://raw.githubusercontent.com/wenwen357951/placeable-fabric/refs/heads/main/docs/img/recipe-large-fern.png" alt="The Large Fern Recipe" width="25%">
</p>

## 🎍 How to use?

When you right-click on a floor block, with some plant it'll be plants

The available plants are:

- Grass and Tall Grass
- Fern and Large Fern
- Cactus
- Sugar Cane
- Bamboo
- Dead Bush
- Cocoa beans
- Sweet Berries
- Big / Small Dripleaf
- Nether Wart
- Nether Sprouts
- Crimson / Warped Roots
- Mangrove Propagule
- Torchflowers
- Pitcher Plant
- ...
- All Flowers
- All Saplings
- All Crops

## 📦 How to install?

1. Download the Mod
2. Navigate to your `mods` folder
3. Put the downloaded `.jar` file ([Placeable Plants]) into this folder

## 🛠️ Configuration

The mod allows you to control whether various plants can be placed anywhere (default: all enabled).

The primary way to configure this is through the mod menu's settings panel.

<img src="https://raw.githubusercontent.com/wenwen357951/placeable-fabric/refs/heads/main/docs/img/placeable-config-setting.png" alt="Grass placed on weird blocks">

For advanced users, the `placeable.json` configuration file can also be directly modified as a secondary method.

```
config/placeable.json

{
  // Enable or disable the mod.
  "enable": true,

  // Allow placement on blocks without a top rim.
  "placedWithoutTopRim": false,

  // Allow or disable specific plants.
  "allowPlaceablePlants": {
    "OAK_SAPLING": true,
    "SPRUCE_SAPLING": true,
    ...
    "NETHER_SPROUTS": true,
    "NETHER_WART": true,
    "LILY_PAD": true
  }
}
```

## 📌 Credits

[Bisumto] the original creator of this feature.


[Placeable Plants]: https://modrinth.com/mod/placeable-plants

[Bisumto]: https://github.com/BisUmTo/placeable
