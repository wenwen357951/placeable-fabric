package it.bisumto.placeable.mixin;

import it.bisumto.placeable.Placeable;
import net.minecraft.block.BambooShootBlock;
import net.minecraft.block.BlockState;
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

@Mixin(BambooShootBlock.class)
public class BambooSaplingBlockMixin {

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
        if (Placeable.isDisable(blockState)) {
            return;
        }

        BlockState floor = world.getBlockState(blockPos.down());
        if (!floor.isIn(BlockTags.BAMBOO_PLANTABLE_ON)) {
            ci.cancel();
        }
    }
}
