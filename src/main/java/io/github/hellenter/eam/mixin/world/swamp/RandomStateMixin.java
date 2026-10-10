package io.github.hellenter.eam.mixin.world.swamp;

import io.github.hellenter.eam.world.swamp.SwampyForestTerrain;

import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.RandomState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Hands the overworld's noise router to the swampy forest, which flattens the terrain under it. */
@Mixin(RandomState.class)
abstract class RandomStateMixin {
	@ModifyVariable(method = "create(Lnet/minecraft/core/HolderGetter;JZLnet/minecraft/world/level/block/state/BlockState;ILnet/minecraft/world/level/levelgen/NoiseRouter;)Lnet/minecraft/world/level/levelgen/RandomState;",
			at = @At("HEAD"), argsOnly = true)
	private static NoiseRouter eam$flattenSwampyForest(NoiseRouter router) {
		return SwampyForestTerrain.flatten(router);
	}
}
