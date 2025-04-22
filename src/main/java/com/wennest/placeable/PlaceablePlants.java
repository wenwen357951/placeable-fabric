package com.wennest.placeable;

import lombok.Getter;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.resource.language.I18n;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

@Getter
public enum PlaceablePlants {
    // Sapling
    OAK_SAPLING(Blocks.OAK_SAPLING),
    SPRUCE_SAPLING(Blocks.SPRUCE_SAPLING),
    BIRCH_SAPLING(Blocks.BIRCH_SAPLING),
    JUNGLE_SAPLING(Blocks.JUNGLE_SAPLING),
    ACACIA_SAPLING(Blocks.ACACIA_SAPLING),
    DARK_OAK_SAPLING(Blocks.DARK_OAK_SAPLING),
    MANGROVE_PROPAGULE(Blocks.MANGROVE_PROPAGULE),
    CHERRY_SAPLING(Blocks.CHERRY_SAPLING),
    PALE_OAK_SAPLING(Blocks.PALE_OAK_SAPLING),
    AZALEA(Blocks.AZALEA),
    FLOWERING_AZALEA(Blocks.FLOWERING_AZALEA),

    // Mushroom
    BROWN_MUSHROOM(Blocks.BROWN_MUSHROOM),
    RED_MUSHROOM(Blocks.RED_MUSHROOM),
    CRIMSON_FUNGUS(Blocks.CRIMSON_FUNGUS),
    WARPED_FUNGUS(Blocks.WARPED_FUNGUS),

    // Grass
    SHORT_GRASS(Blocks.SHORT_GRASS),
    TALL_GRASS(Blocks.TALL_GRASS),
    FERN(Blocks.FERN),
    LARGE_FERN(Blocks.LARGE_FERN),
    DEAD_BUSH(Blocks.DEAD_BUSH),

    // Flowers
    DANDELION(Blocks.DANDELION),
    POPPY(Blocks.POPPY),
    BLUE_ORCHID(Blocks.BLUE_ORCHID),
    ALLIUM(Blocks.ALLIUM),
    AZURE_BLUET(Blocks.AZURE_BLUET),
    RED_TULIP(Blocks.RED_TULIP),
    ORANGE_TULIP(Blocks.ORANGE_TULIP),
    WHITE_TULIP(Blocks.WHITE_TULIP),
    PINK_TULIP(Blocks.PINK_TULIP),
    OXEYE_DAISY(Blocks.OXEYE_DAISY),
    CORNFLOWER(Blocks.CORNFLOWER),
    LILY_OF_THE_VALLEY(Blocks.LILY_OF_THE_VALLEY),
    TORCHFLOWER(Blocks.TORCHFLOWER),
    CLOSED_EYEBLOSSOM(Blocks.CLOSED_EYEBLOSSOM),
    OPEN_EYEBLOSSOM(Blocks.OPEN_EYEBLOSSOM),
    WITHER_ROSE(Blocks.WITHER_ROSE),

    // Tall Flowers
    SUNFLOWER(Blocks.SUNFLOWER),
    LILAC(Blocks.LILAC),
    ROSE_BUSH(Blocks.ROSE_BUSH),
    PEONY(Blocks.PEONY),
    PITCHER_PLANT(Blocks.PITCHER_PLANT),
    BIG_DRIPLEAF(Blocks.BIG_DRIPLEAF),
    SMALL_DRIPLEAF(Blocks.SMALL_DRIPLEAF),

    // Other
    PINK_PETALS(Blocks.PINK_PETALS),

    // Crops
    BAMBOO(Blocks.BAMBOO_SAPLING),
    SUGAR_CANE(Blocks.SUGAR_CANE),
    CACTUS(Blocks.CACTUS),
    WHEAT(Blocks.WHEAT),
    COCOA(Blocks.COCOA),
    PUMPKIN_STEM(Blocks.PUMPKIN_STEM),
    MELON_STEM(Blocks.MELON_STEM),
    BEETROOT(Blocks.BEETROOTS),
    TORCHFLOWER_CROP(Blocks.TORCHFLOWER_CROP),
    PITCHER_CROP(Blocks.PITCHER_CROP),

    SWEET_BERRY_BUSH(Blocks.SWEET_BERRY_BUSH),

    // Nether
    CRIMSON_ROOTS(Blocks.CRIMSON_ROOTS),
    WARPED_ROOTS(Blocks.WARPED_ROOTS),
    NETHER_SPROUTS(Blocks.NETHER_SPROUTS),
    NETHER_WART(Blocks.NETHER_WART),

    // Water
    LILY_PAD(Blocks.LILY_PAD);

    @NotNull
    private final Block block;

    PlaceablePlants(@NotNull Block block) {
        this.block = block;
    }

    @NotNull
    public static Optional<PlaceablePlants> findBy(Block block) {
        for (PlaceablePlants plants : PlaceablePlants.values()) {
            if (plants.getBlock() == block) {
                return Optional.of(plants);
            }
        }
        return Optional.empty();
    }

    public String getTranslationName() {
        return I18n.translate(this.block.getTranslationKey());
    }
}
