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
    <a href="https://discord.gg/yeemo">
        <img src="https://img.shields.io/discord/1141595063567273995?style=for-the-badge" alt="Discord chat" />
    </a>
    <a href="https://modrinth.com/mod/placeable-plants">
        <img src="https://img.shields.io/modrinth/dt/placeable-plants?style=for-the-badge" alt="Modrinth downloads" />
    </a>
    <img src="https://img.shields.io/github/license/wenwen357951/placeable.svg?style=for-the-badge" alt="License" />
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
- Added recipes for Tall Grass and Large Fern

<p align="center">
    <img src="https://i.imgur.com/R6rnrUG.png" alt="The Tall Grass Recipe" width="30%">
    <img src="https://i.imgur.com/pz5Sfsx.png" alt="The Large Fern Recipe" width="30%">
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
- All Flowers
- All Saplings
- All Crops

## 📦 How to install?

1. Download the Mod
2. Navigate to your `mods` folder
3. Put the downloaded `.jar` file ([Placeable Plants]) into this folder

## 🛠️ Configuration

The mod provides a `placeable.json` configuration file, which can be used to turn off whether various plants can be
placed anywhere (default: all enabled)

```
placeable.json

{
    "HOW_TO_DISABLE_PLANTS!!!": [
        "Please remove the '//' at the beginning of the string.",
        "For example, to disable the ability to place cactus anywhere,",
        "change '//minecraft:cactus' to 'minecraft:cactus'"
    ],
    "disable_plants": [
        "minecraft:cactus",
        "//minecraft:spruce_sapling",
        ...
        "//minecraft:lily_pad"
    ]
}
```

## 📌 Credits

[Bisumto] the original creator of this feature.


[Placeable Plants]: https://modrinth.com/mod/placeable-plants

[Bisumto]: https://github.com/BisUmTo/placeable
