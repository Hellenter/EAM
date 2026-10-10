package io.github.hellenter.eam.mixin.world.swamp;

import io.github.hellenter.eam.world.swamp.SwampyForestTerrain;

import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.densityfunction.DensityFunction;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Hands the overworld's noise router to the swampy forest, which lays its plain into the terrain, and makes the chunk
 * generator's own final density (taken from its settings) sample the swamp one.
 */
@Mixin(RandomState.class)
abstract class RandomStateMixin {
	@Shadow
	@Final
	private NoiseRouter router;

	@ModifyVariable(method = "create(Lnet/minecraft/core/HolderGetter;JZLnet/minecraft/world/level/block/state/BlockState;ILnet/minecraft/world/level/levelgen/NoiseRouter;)Lnet/minecraft/world/level/levelgen/RandomState;",
			at = @At("HEAD"), argsOnly = true)
	private static NoiseRouter eam$flattenSwampyForest(NoiseRouter router) {
		return SwampyForestTerrain.flatten(router);
	}

	@ModifyVariable(method = "getSampler", at = @At("HEAD"), argsOnly = true)
	private DensityFunction eam$swampyFinalDensity(DensityFunction function) {
		return SwampyForestTerrain.substitute(this.router, function);
	}
}
