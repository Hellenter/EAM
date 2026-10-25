package io.github.hellenter.eam.mixin.survival.thirst;

import io.github.hellenter.eam.survival.thirst.CauldronBoiling;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.AbstractCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A water cauldron over a fire heats on its scheduled ticks. */
@Mixin(AbstractCauldronBlock.class)
abstract class CauldronBoilMixin {
	@Inject(method = "tick", at = @At("TAIL"))
	private void eam$boil(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
		CauldronBoiling.tick(state, level, pos, random);
	}
}
