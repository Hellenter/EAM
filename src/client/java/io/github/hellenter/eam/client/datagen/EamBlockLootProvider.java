package io.github.hellenter.eam.client.datagen;

import io.github.hellenter.eam.content.EamBlocks;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricBlockLootSubProvider;

import net.minecraft.core.HolderLookup;

import java.util.concurrent.CompletableFuture;

public class EamBlockLootProvider extends FabricBlockLootSubProvider {
	public EamBlockLootProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
		super(output, registries);
	}

	@Override
	public void generate() {
		dropSelf(EamBlocks.PEBBLE);
		dropSelf(EamBlocks.FLINT_SHARD);
	}
}
