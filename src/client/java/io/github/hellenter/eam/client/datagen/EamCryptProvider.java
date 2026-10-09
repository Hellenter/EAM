package io.github.hellenter.eam.client.datagen;

import io.github.hellenter.eam.world.structures.crypt.CryptModule;
import io.github.hellenter.eam.world.structures.crypt.CryptStructure;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricDynamicRegistryProvider;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.TerrainAdjustment;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * The crypt structure: a main path of 7 to 10 rooms with 1 to 3 side passages. The entrance template's 18th layer
 * (17 counted from 0) is its ground floor.
 */
public class EamCryptProvider extends FabricDynamicRegistryProvider {
	public EamCryptProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
		super(output, registries);
	}

	public static void bootstrap(BootstrapContext<Structure> context) {
		var biomes = context.lookup(Registries.BIOME);
		context.register(CryptModule.STRUCTURE, new CryptStructure(
				new Structure.StructureSettings(biomes.getOrThrow(BiomeTags.IS_OVERWORLD), Map.of(),
						GenerationStep.Decoration.UNDERGROUND_STRUCTURES, TerrainAdjustment.ENCAPSULATE),
				UniformInt.of(7, 10), UniformInt.of(1, 3), 17));
	}

	@Override
	protected void configure(HolderLookup.Provider registries, Entries entries) {
		entries.add(registries.lookupOrThrow(Registries.STRUCTURE), CryptModule.STRUCTURE);
	}

	@Override
	public String getName() {
		return "EAM crypt";
	}
}
