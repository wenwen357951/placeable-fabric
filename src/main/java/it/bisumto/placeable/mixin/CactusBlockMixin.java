package it.bisumto.placeable.mixin;

import it.bisumto.placeable.Placeable;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CactusBlock;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.EmptyBlockView;
import net.minecraft.world.WorldView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Iterator;

@Mixin(CactusBlock.class)
public class CactusBlockMixin {

    // PLACEABLE
    @Inject(method = "canPlaceAt", at = @At("HEAD"), cancellable = true)
    public void canPlantAnywhere(BlockState blockState, WorldView world, BlockPos blockPos, CallbackInfoReturnable<Boolean> cir) {
        if (Placeable.isDisable(blockState)) {
            return;
        }

        Iterator<Direction> directionIterator = Direction.Type.HORIZONTAL.iterator();
        Direction direction;
        BlockState directionBlock;
        do {
            if (!directionIterator.hasNext()) {
                BlockState underBlockState = world.getBlockState(blockPos.down());
                if ((underBlockState.isOf(Blocks.CACTUS) || Placeable.isValidFloor(world, blockPos))
                        && world.getFluidState(blockPos.up()).isEmpty()) {
                    cir.setReturnValue(true);
                }
                return;
            }

            direction = directionIterator.next();
            directionBlock = world.getBlockState(blockPos.offset(direction));
        } while (!directionBlock.isSolidBlock(EmptyBlockView.INSTANCE, BlockPos.ORIGIN)
                && !directionBlock.isOpaque()
                && !world.getFluidState(blockPos.offset(direction)).isIn(FluidTags.LAVA));
    }

    // PREVENT GROWING
    @Inject(method = "randomTick", at = @At("HEAD"), cancellable = true)
    public void randomTickMixin(BlockState blockState, ServerWorld world, BlockPos blockPos, Random random, CallbackInfo ci) {
        if (Placeable.isDisable(blockState)) {
            return;
        }

        int i = 1;
        while (i < 3 && world.getBlockState(blockPos.down(i)).isOf(Blocks.CACTUS)) {
            ++i;
        }

        BlockPos groundBlockPos = blockPos.down(i);
        BlockState groundBlockState = world.getBlockState(groundBlockPos);
        if (!groundBlockState.isIn(BlockTags.SAND)) {
            ci.cancel();
        }
    }
}
