package com.wennest.placeable;

import lombok.Getter;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Catalogue of every vanilla {@link Block} that this mod relaxes placement for.
 *
 * <p>The enum doubles as the JSON config keyset (see
 * {@link PlaceableConfig#allowPlaceablePlants}). Renaming an entry is
 * therefore a <em>breaking change</em> for existing user configs and MUST be
 * avoided.
 *
 * <p><b>Lookup performance</b>: {@link #findBy(Block)} runs inside
 * {@code Placeable.isDisabled}, which is itself invoked from every
 * {@code canPlaceAt} mixin. The {@link #BY_BLOCK} map gives O(1) lookup and
 * is built once at class init, avoiding a linear scan on every random-tick /
 * placement check.
 *
 * <p><b>Multi-version support</b>: entries that reference {@code Blocks}
 * constants which do not exist in every supported MC version are wrapped in
 * {@code //? if >=<MC>} comments. Stonecutter strips the line when
 * preprocessing for older versions, so the enum compiles against each target
 * without manual scaffolding. See {@code docs/VERSIONS.md} for the support
 * matrix.
 *
 * <p><b>Intentional exclusions</b>: the following vanilla plant / vegetation
 * blocks exist in some supported MC versions but are deliberately NOT in
 * this enum — placing them is outside the mod's mission:
 * <ul>
 *   <li>{@code MOSS_CARPET}, {@code PALE_MOSS_CARPET} — carpet-style cover
 *       blocks; vanilla placement rules are already permissive enough.</li>
 *   <li>{@code SPORE_BLOSSOM} — ceiling-attached, not floor-placed; the
 *       {@code isValidFloor} model does not apply.</li>
 *   <li>{@code GLOW_LICHEN}, {@code PALE_HANGING_MOSS}, {@code HANGING_ROOTS},
 *       {@code VINE}, {@code CAVE_VINES}, {@code TWISTING_VINES},
 *       {@code WEEPING_VINES}, {@code KELP_PLANT}, {@code SEAGRASS},
 *       {@code TALL_SEAGRASS} — multi-face / underwater / hanging blocks
 *       outside the "place a plant on top of any block" mission.</li>
 *   <li>{@code CHORUS_PLANT}, {@code CHORUS_FLOWER} — End-only; vanilla
 *       placement is already player-permissive.</li>
 *   <li>{@code POTTED_*} variants — flowerpot contents, never a player-placed
 *       block in the world sense.</li>
 *   <li>Any non-plant vegetation-named block (e.g., {@code BAMBOO_SHELF},
 *       {@code MUSHROOM_STEM}, {@code *_LEAVES}) — block, not plant.</li>
 * </ul>
 */
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
    // PALE_OAK_SAPLING was introduced in 1.21.4.
    //? if >=1.21.4
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
    // SHORT_DRY_GRASS / TALL_DRY_GRASS were introduced in 1.21.5.
    //? if >=1.21.5
    SHORT_DRY_GRASS(Blocks.SHORT_DRY_GRASS),
    //? if >=1.21.5
    TALL_DRY_GRASS(Blocks.TALL_DRY_GRASS),
    FERN(Blocks.FERN),
    LARGE_FERN(Blocks.LARGE_FERN),
    // BUSH and FIREFLY_BUSH were introduced in 1.21.5.
    //? if >=1.21.5
    BUSH(Blocks.BUSH),
    //? if >=1.21.5
    FIREFLY_BUSH(Blocks.FIREFLY_BUSH),
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
    // CLOSED_EYEBLOSSOM and OPEN_EYEBLOSSOM were introduced in 1.21.4.
    //? if >=1.21.4
    CLOSED_EYEBLOSSOM(Blocks.CLOSED_EYEBLOSSOM),
    //? if >=1.21.4
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
    // WILDFLOWERS was introduced in 1.21.5.
    //? if >=1.21.5
    WILDFLOWERS(Blocks.WILDFLOWERS),
    // LEAF_LITTER was introduced in 1.21.5. Vanilla canPlaceAt requires a
    // fully-solid top face (rejects slabs / stairs / leaves / dirt-path);
    // LeafLitterBlockMixin relaxes that to the standard isValidFloor rule.
    //? if >=1.21.5
    LEAF_LITTER(Blocks.LEAF_LITTER),

    // Crops
    BAMBOO(Blocks.BAMBOO_SAPLING),
    SUGAR_CANE(Blocks.SUGAR_CANE),
    CACTUS(Blocks.CACTUS),
    // CACTUS_FLOWER was introduced in 1.21.5; placement on top of any cactus
    // or valid floor follows the same isValidFloor rules.
    //? if >=1.21.5
    CACTUS_FLOWER(Blocks.CACTUS_FLOWER),
    WHEAT(Blocks.WHEAT),
    COCOA(Blocks.COCOA),
    PUMPKIN_STEM(Blocks.PUMPKIN_STEM),
    MELON_STEM(Blocks.MELON_STEM),
    BEETROOT(Blocks.BEETROOTS),
    POTATOES(Blocks.POTATOES),
    CARROTS(Blocks.CARROTS),
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

    /**
     * Precomputed Block→PlaceablePlants index. Built once at class init from
     * {@link #values()} and exposed as an unmodifiable view so callers cannot
     * mutate the lookup table at runtime.
     *
     * <p>Two enum entries that map to the same {@link Block} (none currently)
     * would collapse in this map; the first-write-wins semantics is
     * acceptable because such a duplication would already be a code error.
     */
    private static final Map<Block, PlaceablePlants> BY_BLOCK;

    static {
        // HashMap (not ImmutableMap from Guava) avoids dragging an extra
        // dependency: Guava is already on the classpath transitively but the
        // build script does not declare it as a direct dep, so the JDK's
        // `Collections.unmodifiableMap` + `HashMap` pattern is preferred.
        Map<Block, PlaceablePlants> tmp = new HashMap<>(values().length * 2);
        for (PlaceablePlants p : values()) {
            tmp.put(p.block, p);
        }
        BY_BLOCK = Collections.unmodifiableMap(tmp);
    }

    @NotNull
    private final Block block;

    PlaceablePlants(@NotNull Block block) {
        this.block = block;
    }

    /**
     * Resolve a {@link Block} instance to its {@link PlaceablePlants} entry.
     *
     * <p><b>Contract</b>: returns {@link Optional#empty()} for an unknown
     * block AND for a {@code null} input.
     *
     * @param block the vanilla block to look up; may be {@code null}.
     * @return the matching enum entry wrapped in {@code Optional}, or empty.
     */
    @NotNull
    public static Optional<PlaceablePlants> findBy(@Nullable Block block) {
        if (block == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(BY_BLOCK.get(block));
    }

    /**
     * Test-only / introspection-only accessor for the precomputed lookup map.
     * Returned map is unmodifiable. Production callers should use
     * {@link #findBy(Block)} instead of touching this directly.
     */
    @NotNull
    public static Map<Block, PlaceablePlants> byBlockView() {
        return BY_BLOCK;
    }

    // Localized display names live in
    // com.wennest.placeable.integration.ModMenuIntegration as a private static
    // helper. I18n is a client-only class
    // (net.minecraft.client.resource.language.I18n); referencing it from this
    // server-loadable enum would risk NoClassDefFoundError on a dedicated
    // server. The enum therefore imports no client class.
}
