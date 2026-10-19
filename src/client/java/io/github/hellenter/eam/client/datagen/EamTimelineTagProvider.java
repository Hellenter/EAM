package io.github.hellenter.eam.client.datagen;

import io.github.hellenter.eam.world.darkness.DarknessModule;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagsProvider;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TimelineTags;
import net.minecraft.world.timeline.Timeline;

import java.util.concurrent.CompletableFuture;

public class EamTimelineTagProvider extends FabricTagsProvider<Timeline> {
	public EamTimelineTagProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
		super(output, Registries.TIMELINE, registries);
	}

	@Override
	protected void addTags(HolderLookup.Provider registries) {
		// The overworld and the single-biome caves world both run the darkness timeline next to vanilla's day and moon.
		builder(TimelineTags.IN_OVERWORLD).add(DarknessModule.TIMELINE);
	}
}
