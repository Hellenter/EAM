package io.github.hellenter.eam.client.datagen;

import io.github.hellenter.eam.Eam;
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
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadType;
import net.minecraft.world.level.levelgen.structure.TerrainAdjustment;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

/**
 * The crypt structure: three levels, each with a main path of 7 to 10 rooms and 1 to 3 side passages, and 2 or 3 big
 * rooms in all. The entrance template's 18th layer (17 counted from 0) is its ground floor. {@code eam:crypt} generates
 * by abandoned villages, on the villages' own grid; {@code eam:crypt_test} is the same crypt for {@code /place}.
 */
public class EamCryptProvider extends FabricDynamicRegistryProvider {
	public EamCryptProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
		super(output, registries);
	}

	public static void bootstrap(BootstrapContext<Structure> context) {
		// One village in six before the iron age, when all of them are abandoned; after it one abandoned village in
		// three (a quarter of villages stay abandoned).
		CryptStructure.VillageRule villages = new CryptStructure.VillageRule(1F / 6F, 1F / 3F, 0.25F);
		context.register(CryptModule.STRUCTURE, crypt(context, Optional.of(villages)));
		context.register(CryptModule.TEST_STRUCTURE, crypt(context, Optional.empty()));
	}

	private static CryptStructure crypt(BootstrapContext<Structure> context, Optional<CryptStructure.VillageRule> villages) {
		var biomes = context.lookup(Registries.BIOME);
		return new CryptStructure(
				new Structure.StructureSettings(biomes.getOrThrow(BiomeTags.IS_OVERWORLD), Map.of(),
						GenerationStep.Decoration.UNDERGROUND_STRUCTURES, TerrainAdjustment.ENCAPSULATE),
				UniformInt.of(7, 10), UniformInt.of(1, 3), 17,
				// Maxim's harder rooms wait for the second level.
				Stream.of("treasury/4", "treasury/5", "fight/5", "fight/6", "big/2").map(room -> Eam.id("crypt/" + room)).toList(),
				// The harder rooms turn up more often the deeper the level: half as often again on the second, twice on
				// the third.
				List.of(100, 150, 200),
				3, UniformInt.of(2, 3),
				// Tier-2 treasuries: none on the first level (they are deep rooms anyway), at most two on the second.
				List.of(0, 2), villages,
				// Maxim is reworking the first entrance.
				List.of(Eam.id("crypt/entrance/1")));
	}

	/** Crypts try the same chunks as villages: same spacing, separation and salt as {@code minecraft:villages}. */
	public static void bootstrapSets(BootstrapContext<StructureSet> context) {
		context.register(CryptModule.STRUCTURE_SET, new StructureSet(
				context.lookup(Registries.STRUCTURE).getOrThrow(CryptModule.STRUCTURE),
				new RandomSpreadStructurePlacement(34, 8, RandomSpreadType.LINEAR, 10387312)));
	}

	@Override
	protected void configure(HolderLookup.Provider registries, Entries entries) {
		entries.add(registries.lookupOrThrow(Registries.STRUCTURE), CryptModule.STRUCTURE);
		entries.add(registries.lookupOrThrow(Registries.STRUCTURE), CryptModule.TEST_STRUCTURE);
		entries.add(registries.lookupOrThrow(Registries.STRUCTURE_SET), CryptModule.STRUCTURE_SET);
	}

	@Override
	public String getName() {
		return "EAM crypt";
	}
}
