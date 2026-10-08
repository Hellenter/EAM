package io.github.hellenter.eam.client.datagen;

import io.github.hellenter.eam.world.structures.VillageModule;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagsProvider;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.DesertVillagePools;
import net.minecraft.data.worldgen.PlainVillagePools;
import net.minecraft.data.worldgen.SavannaVillagePools;
import net.minecraft.data.worldgen.SnowyVillagePools;
import net.minecraft.data.worldgen.TaigaVillagePools;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;

import java.util.concurrent.CompletableFuture;

public class EamTemplatePoolTagProvider extends FabricTagsProvider<StructureTemplatePool> {
	public EamTemplatePoolTagProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
		super(output, Registries.TEMPLATE_POOL, registries);
	}

	@Override
	protected void addTags(HolderLookup.Provider registries) {
		builder(VillageModule.GATED_VILLAGE_STARTS)
				.add(PlainVillagePools.START, DesertVillagePools.START, SavannaVillagePools.START, SnowyVillagePools.START,
						TaigaVillagePools.START);
	}
}
