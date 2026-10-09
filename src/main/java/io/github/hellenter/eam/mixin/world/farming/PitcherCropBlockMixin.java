package io.github.hellenter.eam.mixin.world.farming;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import io.github.hellenter.eam.world.farming.FarmingModule;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BonemealSource;
import net.minecraft.world.level.block.PitcherCropBlock;
import net.minecraft.world.level.block.state.BlockState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PitcherCropBlock.class)
abstract class PitcherCropBlockMixin {
	@WrapOperation(method = "randomTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/RandomSource;nextInt(I)I"))
	private int eam$slowGrowth(RandomSource random, int bound, Operation<Integer> original, @Local(argsOnly = true) ServerLevel level,
			@Local(argsOnly = true) BlockPos pos) {
		return original.call(random, FarmingModule.growthBound(level, pos, bound));
	}

	@ModifyReturnValue(method = "isValidBonemealTarget", at = @At("RETURN"))
	private boolean eam$boostOnce(boolean valid, @Local(argsOnly = true) LevelReader level, @Local(argsOnly = true) BlockPos pos,
			@Local(argsOnly = true) BlockState state) {
		return FarmingModule.canBoneMeal(level, FarmingModule.pitcherRoot(pos, state), valid);
	}

	@WrapMethod(method = "performBonemeal")
	private void eam$boneMeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state,
			BonemealSource source, Operation<Void> original) {
		if (!FarmingModule.boneMeal(level, FarmingModule.pitcherRoot(pos, state))) original.call(level, random, pos, state, source);
	}
}
