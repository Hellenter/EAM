package io.github.hellenter.eam.client.datagen;

import io.github.hellenter.eam.world.structures.castle.CastleModule;
import io.github.hellenter.eam.world.structures.castle.CastlePlacement;
import io.github.hellenter.eam.world.structures.castle.CastleStructure;
import io.github.hellenter.eam.world.structures.outpost.OutpostModule;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricDynamicRegistryProvider;

import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.structure.BuiltinStructureSets;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.TerrainAdjustment;
import net.minecraft.world.level.levelgen.structure.placement.AbstractSpreadingStructurePlacement;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * The Pillager King's castle: Maxim's castle in plains and forests, placed after the trees so it can clear them within
 * 15 blocks, on ground rising at most 10 blocks across its footprint. One castle tries every 64 chunks (1024 blocks),
 * at least 24 chunks apart, 10 chunks from villages and 8 from outposts.
 */
public class EamCastleProvider extends FabricDynamicRegistryProvider {
	public EamCastleProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
		super(output, registries);
	}

	public static void bootstrap(BootstrapContext<Structure> context) {
		context.register(CastleModule.STRUCTURE, new CastleStructure(
				new Structure.StructureSettings(context.lookup(Registries.BIOME).getOrThrow(CastleModule.HAS_CASTLE), Map.of(),
						GenerationStep.Decoration.TOP_LAYER_MODIFICATION, TerrainAdjustment.BEARD_THIN),
				15, 10));
	}

	public static void bootstrapSets(BootstrapContext<StructureSet> context) {
		HolderGetter<StructureSet> sets = context.lookup(Registries.STRUCTURE_SET);
		context.register(CastleModule.STRUCTURE_SET, new StructureSet(
				context.lookup(Registries.STRUCTURE).getOrThrow(CastleModule.STRUCTURE),
				new CastlePlacement(64, 24, 1.0F, 20261018, List.of(
						new AbstractSpreadingStructurePlacement.ExclusionZone(sets.getOrThrow(BuiltinStructureSets.VILLAGES), 10),
						new AbstractSpreadingStructurePlacement.ExclusionZone(sets.getOrThrow(OutpostModule.STRUCTURE_SET), 8)))));
	}

	@Override
	protected void configure(HolderLookup.Provider registries, Entries entries) {
		entries.add(registries.lookupOrThrow(Registries.STRUCTURE), CastleModule.STRUCTURE);
		entries.add(registries.lookupOrThrow(Registries.STRUCTURE_SET), CastleModule.STRUCTURE_SET);
	}

	@Override
	public String getName() {
		return "EAM Pillager King castle";
	}
}
