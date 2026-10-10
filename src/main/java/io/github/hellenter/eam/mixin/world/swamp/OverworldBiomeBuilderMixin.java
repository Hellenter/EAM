package io.github.hellenter.eam.mixin.world.swamp;

import io.github.hellenter.eam.world.swamp.SwampyForestBiomes;

import com.mojang.datafixers.util.Pair;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.OverworldBiomeBuilder;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

/** Lists the swampy forest biomes in the overworld biome list, so the overworld's chunk generator knows their features. */
@Mixin(OverworldBiomeBuilder.class)
abstract class OverworldBiomeBuilderMixin {
	@Inject(method = "addBiomes", at = @At("TAIL"))
	private void eam$addSwampyForest(Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> biomes, CallbackInfo ci) {
		SwampyForestBiomes.addToOverworldList(biomes);
	}
}
