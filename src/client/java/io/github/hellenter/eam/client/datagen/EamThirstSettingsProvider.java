package io.github.hellenter.eam.client.datagen;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.survival.thirst.ThirstSettings;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricCodecDataProvider;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

/** Writes the default thirst balance to {@code data/eam/thirst/settings.json} so datapacks see what to override. */
public class EamThirstSettingsProvider extends FabricCodecDataProvider<ThirstSettings> {
	public EamThirstSettingsProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
		super(output, registries, PackOutput.Target.DATA_PACK, "thirst", ThirstSettings.CODEC);
	}

	@Override
	protected void configure(BiConsumer<Identifier, ThirstSettings> provider, HolderLookup.Provider registries) {
		provider.accept(Eam.id("settings"), ThirstSettings.DEFAULT);
	}

	@Override
	public String getName() {
		return "EAM thirst settings";
	}
}
