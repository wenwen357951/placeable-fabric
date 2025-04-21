package com.wennest.placeable;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ModInitializer;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import net.minecraft.world.WorldView;
import net.minecraft.world.chunk.ChunkStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;


public class Placeable implements ModInitializer {
    public static final String MODID = "placeable";
    public static final Logger LOGGER = LoggerFactory.getLogger(MODID);

    @Override
    public void onInitialize() {
        long loadTook = System.currentTimeMillis();
        AutoConfig.register(PlaceableConfig.class, GsonConfigSerializer::new);
        LOGGER.info("Mod loaded in {} ms!", System.currentTimeMillis() - loadTook);
    }

    public static boolean isValidFloor(WorldView world, BlockPos pos) {
        if (world.getChunk(pos).getStatus() != ChunkStatus.FULL) {
            return false;
        }

        return isValidFloor(world.getBlockState(pos.down()), world, pos.down());
    }

    public static boolean isValidFloor(BlockState floor, BlockView world, BlockPos pos) {
        return (getConfig().placedWithoutTopRim || Block.hasTopRim(world, pos))
                || floor.isIn(BlockTags.LEAVES)
                || floor.isOf(Blocks.DIRT_PATH);
    }

    public static boolean isDisable(BlockState blockState) {
        return isDisable(blockState.getBlock());
    }

    public static boolean isDisable(WorldView world, BlockPos blockPos) {
        if (world.getChunk(blockPos).getStatus() != ChunkStatus.FULL) {
            return true;
        }

        return isDisable(world.getBlockState(blockPos));
    }

    public static boolean isDisable(Block block) {
        PlaceableConfig config = getConfig();
        if (config == null || !config.enable) {
            return true;
        }

        Optional<PlaceablePlants> placeablePlants = PlaceablePlants.findBy(block);
        return placeablePlants.isEmpty() || !config.allowPlaceablePlants.getOrDefault(placeablePlants.get(), false);
    }

    public static PlaceableConfig getConfig() {
        return AutoConfig.getConfigHolder(PlaceableConfig.class).get();
    }
}
