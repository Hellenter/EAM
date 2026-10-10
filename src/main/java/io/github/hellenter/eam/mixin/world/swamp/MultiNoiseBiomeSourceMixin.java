package io.github.hellenter.eam.mixin.world.swamp;

import io.github.hellenter.eam.world.swamp.SwampyForestBiomes;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Lets the swampy forest take over land biomes inside its regions, for single lookups and whole chunks alike. */
@Mixin(MultiNoiseBiomeSource.class)
abstract class MultiNoiseBiomeSourceMixin {
	@ModifyReturnValue(method = "createResolver", at = @At("RETURN"))
	private BiomeResolver eam$swampyForest(BiomeResolver original, Climate.Sampler sampler) {
		return SwampyForestBiomes.wrap(original, sampler, (MultiNoiseBiomeSource) (Object) this, false);
	}

	@ModifyReturnValue(method = "createResolverForChunk", at = @At("RETURN"))
	private BiomeResolver eam$swampyForestInChunk(BiomeResolver original, Climate.Sampler sampler) {
		return SwampyForestBiomes.wrap(original, sampler, (MultiNoiseBiomeSource) (Object) this, true);
	}
}
