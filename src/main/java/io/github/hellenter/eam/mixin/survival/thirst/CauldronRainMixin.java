package io.github.hellenter.eam.mixin.survival.thirst;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import io.github.hellenter.eam.survival.thirst.RainWater;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CauldronBlock;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Water that rain adds to a cauldron counts as rain water. */
@Mixin({CauldronBlock.class, LayeredCauldronBlock.class})
abstract class CauldronRainMixin {
	@WrapOperation(method = "handlePrecipitation", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/level/Level;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"))
	private boolean eam$fillWithRain(Level level, BlockPos pos, BlockState state, Operation<Boolean> original) {
		return RainWater.fillWithRain(() -> original.call(level, pos, state));
	}
}
