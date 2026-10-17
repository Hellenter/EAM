package io.github.hellenter.eam.mixin.survival.thirst;

import io.github.hellenter.eam.survival.thirst.RainWater;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Every way water enters or leaves a cauldron ends in a block state change; the rain water mark follows it. */
@Mixin(BlockBehaviour.BlockStateBase.class)
abstract class CauldronWaterChangeMixin {
	@Inject(method = "onPlace", at = @At("HEAD"))
	private void eam$trackRainWater(Level level, BlockPos pos, BlockState oldState, boolean movedByPiston, CallbackInfo ci) {
		RainWater.onChanged(level, pos, (BlockState) (Object) this, oldState);
	}
}
