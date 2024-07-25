package it.bisumto.placeable.mixin;

import it.bisumto.placeable.Placeable;
import net.minecraft.block.BambooBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.WorldView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BambooBlock.class)
public class BambooBlockMixin {

    // PLACEABLE
    @Inject(method = "canPlaceAt", at = @At("HEAD"), cancellable = true)
    public void canPlantAnywhere(BlockState blockState, WorldView world, BlockPos blockPos, CallbackInfoReturnable<Boolean> cir) {
        if (Placeable.isDisable(blockState)) {
            return;
        }

        if (Placeable.isValidFloor(world, blockPos)) {
            cir.setReturnValue(true);
        }
    }

    // PREVENT GROWING
    @Inject(method = "randomTick", at = @At("HEAD"), cancellable = true)
    public void randomTickMixin(BlockState blockState, ServerWorld world, BlockPos blockPos, Random random, CallbackInfo ci) {
        if (Placeable.isDisable(Blocks.BAMBOO_SAPLING)) {
            return;
        }

        int i = 1;
        while (world.getBlockState(blockPos.down(i)).isOf(Blocks.BAMBOO)) {
            i++;
        }

        BlockState floor = world.getBlockState(blockPos.down(i));
        if (!floor.isIn(BlockTags.BAMBOO_PLANTABLE_ON)) {
            ci.cancel();
        }
    }

    // PLACEMENT STATE
    @Inject(method = "getPlacementState", at = @At("TAIL"), cancellable = true)
    public void getPlacementStateMixin(ItemPlacementContext ctx, CallbackInfoReturnable<BlockState> cir) {
        if (Placeable.isDisable(Blocks.BAMBOO_SAPLING)) {
            return;
        }

        if (Placeable.isValidFloor(ctx.getWorld(), ctx.getBlockPos())) {
            cir.setReturnValue(Blocks.BAMBOO_SAPLING.getDefaultState());
        }
    }
}
