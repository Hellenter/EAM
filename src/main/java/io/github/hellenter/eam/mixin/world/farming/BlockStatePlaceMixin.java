package io.github.hellenter.eam.mixin.world.farming;

import io.github.hellenter.eam.world.farming.FarmingModule;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A crop planted where a boosted one stood does not inherit its bone meal boost. */
@Mixin(BlockBehaviour.BlockStateBase.class)
abstract class BlockStatePlaceMixin {
	@Inject(method = "onPlace", at = @At("HEAD"))
	private void eam$forgetBoost(Level level, BlockPos pos, BlockState oldState, boolean movedByPiston, CallbackInfo ci) {
		FarmingModule.onPlaced(level, pos, (BlockState) (Object) this, oldState);
	}
}
