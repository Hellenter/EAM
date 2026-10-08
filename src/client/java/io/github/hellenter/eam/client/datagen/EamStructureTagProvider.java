package io.github.hellenter.eam.client.datagen;

import io.github.hellenter.eam.world.structures.StructureRemovalModule;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagsProvider;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.concurrent.CompletableFuture;

public class EamStructureTagProvider extends FabricTagsProvider<Structure> {
	public EamStructureTagProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
		super(output, Registries.STRUCTURE, registries);
	}

	@Override
	protected void addTags(HolderLookup.Provider registries) {
		// The mace will come from a different monument.
		builder(StructureRemovalModule.REMOVED)
				.add(BuiltinStructures.TRIAL_CHAMBERS);
	}
}
