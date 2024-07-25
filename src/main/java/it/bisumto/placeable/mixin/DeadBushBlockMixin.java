package it.bisumto.placeable.mixin;

import it.bisumto.placeable.Placeable;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.DeadBushBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DeadBushBlock.class)
public class DeadBushBlockMixin {

    // PLACEABLE
    @Inject(method = "canPlantOnTop", at = @At("HEAD"), cancellable = true)
    public void canPlantAnywhere(BlockState blockState, BlockView world, BlockPos blockPos, CallbackInfoReturnable<Boolean> cir) {
        if (Placeable.isDisable(Blocks.DEAD_BUSH)) {
            return;
        }

        if (Placeable.isValidFloor(blockState, world, blockPos)) {
            cir.setReturnValue(true);
        }
    }
}
