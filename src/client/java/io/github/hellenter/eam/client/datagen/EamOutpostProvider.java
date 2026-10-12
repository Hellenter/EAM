package io.github.hellenter.eam.client.datagen;

import io.github.hellenter.eam.world.structures.outpost.OutpostModule;
import io.github.hellenter.eam.world.structures.outpost.OutpostStructure;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricDynamicRegistryProvider;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.structure.BuiltinStructureSets;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.TerrainAdjustment;
import net.minecraft.world.level.levelgen.structure.placement.AbstractSpreadingStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadType;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * The pillager outpost: Maxim's template in plains and forests, placed after the trees so it can clear them within 15
 * blocks. It is spread like the vanilla outpost (which no longer generates) and keeps the same 10 chunks from villages.
 */
public class EamOutpostProvider extends FabricDynamicRegistryProvider {
	public EamOutpostProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
		super(output, registries);
	}

	public static void bootstrap(BootstrapContext<Structure> context) {
		context.register(OutpostModule.STRUCTURE, new OutpostStructure(
				new Structure.StructureSettings(context.lookup(Registries.BIOME).getOrThrow(OutpostModule.HAS_OUTPOST), Map.of(),
						GenerationStep.Decoration.TOP_LAYER_MODIFICATION, TerrainAdjustment.BEARD_THIN),
				15, 10));
	}

	public static void bootstrapSets(BootstrapContext<StructureSet> context) {
		context.register(OutpostModule.STRUCTURE_SET, new StructureSet(
				context.lookup(Registries.STRUCTURE).getOrThrow(OutpostModule.STRUCTURE),
				new RandomSpreadStructurePlacement(Vec3i.ZERO, AbstractSpreadingStructurePlacement.FrequencyReductionMethod.DEFAULT, 0.2F,
						20261012, Optional.of(new AbstractSpreadingStructurePlacement.ExclusionZone(
								context.lookup(Registries.STRUCTURE_SET).getOrThrow(BuiltinStructureSets.VILLAGES), 10)),
						32, 8, RandomSpreadType.LINEAR)));
	}

	@Override
	protected void configure(HolderLookup.Provider registries, Entries entries) {
		entries.add(registries.lookupOrThrow(Registries.STRUCTURE), OutpostModule.STRUCTURE);
		entries.add(registries.lookupOrThrow(Registries.STRUCTURE_SET), OutpostModule.STRUCTURE_SET);
	}

	@Override
	public String getName() {
		return "EAM pillager outpost";
	}
}
