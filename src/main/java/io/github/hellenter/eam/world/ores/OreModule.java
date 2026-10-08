package io.github.hellenter.eam.world.ores;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.content.EamWorldgen;

import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.ModificationPhase;

import net.minecraft.data.worldgen.placement.OrePlacements;
import net.minecraft.world.level.levelgen.GenerationStep;

/**
 * Ore generation changes. Copper: vanilla's copper veins (and the large ones in dripstone caves) are replaced by
 * {@link EamWorldgen#ORE_COPPER_PLACED}, with fewer veins per chunk and diamond-sized veins. Drops are datagen loot tables.
 */
public final class OreModule {
	private OreModule() {
	}

	public static void init() {
		BiomeModifications.create(Eam.id("copper_ore")).add(ModificationPhase.REPLACEMENTS,
				biome -> biome.hasPlacedFeature(OrePlacements.ORE_COPPER) || biome.hasPlacedFeature(OrePlacements.ORE_COPPER_LARGE),
				context -> {
					var generation = context.getGenerationSettings();
					generation.removeFeature(OrePlacements.ORE_COPPER);
					generation.removeFeature(OrePlacements.ORE_COPPER_LARGE);
					generation.addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, EamWorldgen.ORE_COPPER_PLACED);
				});
	}
}
