package io.github.hellenter.eam.client.datagen;

import io.github.hellenter.eam.content.EamItems;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagsProvider;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.Items;

import java.util.concurrent.CompletableFuture;

public class EamItemTagProvider extends FabricTagsProvider.ItemTagsProvider {
	public EamItemTagProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
		super(output, registries);
	}

	@Override
	protected void addTags(HolderLookup.Provider registries) {
		builder(EamItems.FLASK_THREADS).add(EamItems.LEATHER_THREAD.builtInRegistryHolder().key(), Items.STRING.builtInRegistryHolder().key());
	}
}
