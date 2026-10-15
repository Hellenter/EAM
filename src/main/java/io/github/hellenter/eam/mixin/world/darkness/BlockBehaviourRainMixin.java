package io.github.hellenter.eam.mixin.world.darkness;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import io.github.hellenter.eam.world.darkness.RainDousing;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Torches and campfires have no random tick of their own; they get one so the rain can put them out. */
@Mixin(BlockBehaviour.class)
abstract class BlockBehaviourRainMixin {
	@ModifyReturnValue(method = "isRandomlyTicking", at = @At("RETURN"))
	private boolean eam$tickInRain(boolean original) {
		return original || RainDousing.isDousable((BlockBehaviour) (Object) this);
	}

	@Inject(method = "randomTick", at = @At("HEAD"))
	private void eam$douse(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
		RainDousing.randomTick(state, level, pos, random);
	}
}
