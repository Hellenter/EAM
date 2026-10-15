package io.github.hellenter.eam.mixin.survival.thirst;

import io.github.hellenter.eam.survival.thirst.RawWater;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BrewingStandBlockEntity.class)
abstract class BrewingStandBoilMixin {
	/** A brew boils every bottle in the stand. */
	@Inject(method = "doBrew", at = @At("TAIL"))
	private static void eam$boilBottles(ServerLevel level, BlockPos pos, BrewingStandBlockEntity entity, CallbackInfo ci) {
		for (int slot = 0; slot < 3; slot++) {
			RawWater.boilAfterBrewing(entity.getItem(slot));
		}
	}
}
