package io.github.hellenter.eam.mixin.world.swamp;

import io.github.hellenter.eam.world.swamp.SwampyForestTerrain;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.RandomState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Aquifers read the preliminary surface; under the swampy forest that is the flattened one. */
@Mixin(NoiseChunk.class)
abstract class NoiseChunkMixin {
	@ModifyExpressionValue(method = "<init>", at = @At(value = "INVOKE", target = "Ljava/util/Optional;get()Ljava/lang/Object;"))
	private Object eam$flatSwampAquifers(Object aquifers, @Local(argsOnly = true) RandomState randomState) {
		return SwampyForestTerrain.aquifers(((RandomStateAccessor) (Object) randomState).eam$router(), (Aquifer.Config) aquifers);
	}
}
