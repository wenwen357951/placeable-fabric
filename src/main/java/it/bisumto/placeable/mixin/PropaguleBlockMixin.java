package it.bisumto.placeable.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.PropaguleBlock;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PropaguleBlock.class)
public class PropaguleBlockMixin {

    @Shadow
    @Final
    public static BooleanProperty HANGING;

    // PREVENT GROWING
    @Inject(method = "randomTick", at = @At("HEAD"), cancellable = true)
    public void randomTickMixin(BlockState blockState, ServerWorld world, BlockPos blockPos, Random random, CallbackInfo ci) {
        if (blockState.get(HANGING)) {
            return;
        }

        BlockState underBlockState = world.getBlockState(blockPos.down());
        if ((underBlockState.isIn(BlockTags.DIRT) && !underBlockState.isOf(Blocks.DIRT_PATH))
                || underBlockState.isOf(Blocks.MOSS_BLOCK)
                || underBlockState.isOf(Blocks.MUD)) {
            return;
        }

        ci.cancel();
    }
}
