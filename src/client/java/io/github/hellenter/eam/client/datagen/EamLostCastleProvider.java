package io.github.hellenter.eam.client.datagen;

import io.github.hellenter.eam.world.structures.castle.CastlePlacement;
import io.github.hellenter.eam.world.structures.lostcastle.LostCastleModule;
import io.github.hellenter.eam.world.structures.lostcastle.LostCastleStructure;
import io.github.hellenter.eam.world.structures.mine.UnderworldMine;
import io.github.hellenter.eam.world.structures.village.DuergarVillage;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricDynamicRegistryProvider;

import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.TerrainAdjustment;
import net.minecraft.world.level.levelgen.structure.placement.AbstractSpreadingStructurePlacement;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * The Lost castle: Maxim's fort on the floor of the underworld's stone wastes, between y 58 and 150. One castle tries
 * every 40 chunks (640 blocks), at least 16 chunks apart, and only where the wastes have a wide open floor, so it is
 * rare; it keeps 8 chunks from duergar villages and 3 from mines. It is placed after the cave's pillars, lakes and
 * mushrooms, so none of them grows through it.
 */
public class EamLostCastleProvider extends FabricDynamicRegistryProvider {
	public EamLostCastleProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
		super(output, registries);
	}

	public static void bootstrap(BootstrapContext<Structure> context) {
		context.register(LostCastleModule.STRUCTURE, new LostCastleStructure(
				new Structure.StructureSettings(context.lookup(Registries.BIOME).getOrThrow(LostCastleModule.HAS_CASTLE), Map.of(),
						GenerationStep.Decoration.TOP_LAYER_MODIFICATION, TerrainAdjustment.NONE),
				58, 150));
	}

	public static void bootstrapSets(BootstrapContext<StructureSet> context) {
		HolderGetter<StructureSet> sets = context.lookup(Registries.STRUCTURE_SET);
		context.register(LostCastleModule.STRUCTURE_SET, new StructureSet(
				context.lookup(Registries.STRUCTURE).getOrThrow(LostCastleModule.STRUCTURE),
				new CastlePlacement(40, 16, 1.0F, 20261024, List.of(
						new AbstractSpreadingStructurePlacement.ExclusionZone(sets.getOrThrow(DuergarVillage.STRUCTURE_SET), 8),
						new AbstractSpreadingStructurePlacement.ExclusionZone(sets.getOrThrow(UnderworldMine.STRUCTURE_SET), 3)))));
	}

	@Override
	protected void configure(HolderLookup.Provider registries, Entries entries) {
		entries.add(registries.lookupOrThrow(Registries.STRUCTURE), LostCastleModule.STRUCTURE);
		entries.add(registries.lookupOrThrow(Registries.STRUCTURE_SET), LostCastleModule.STRUCTURE_SET);
	}

	@Override
	public String getName() {
		return "EAM Lost castle";
	}
}
