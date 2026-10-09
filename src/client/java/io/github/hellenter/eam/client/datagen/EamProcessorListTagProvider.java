package io.github.hellenter.eam.client.datagen;

import io.github.hellenter.eam.world.structures.VillageModule;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagsProvider;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.ProcessorLists;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;

import java.util.concurrent.CompletableFuture;

public class EamProcessorListTagProvider extends FabricTagsProvider<StructureProcessorList> {
	public EamProcessorListTagProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
		super(output, Registries.PROCESSOR_LIST, registries);
	}

	@Override
	protected void addTags(HolderLookup.Provider registries) {
		// Every abandoned village piece uses its biome's zombie processor list, including the normal houses it borrows.
		builder(VillageModule.ABANDONED_PIECES)
				.add(ProcessorLists.ZOMBIE_PLAINS, ProcessorLists.ZOMBIE_DESERT, ProcessorLists.ZOMBIE_SAVANNA, ProcessorLists.ZOMBIE_SNOWY,
						ProcessorLists.ZOMBIE_TAIGA);
	}
}
