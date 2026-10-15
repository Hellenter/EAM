package io.github.hellenter.eam.client.datagen;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.survival.spoilage.SpoilageSettings;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricCodecDataProvider;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

/** Writes the default spoilage and fridge balance to {@code data/eam/spoilage/settings.json} so datapacks see what to override. */
public class EamSpoilageSettingsProvider extends FabricCodecDataProvider<SpoilageSettings> {
	public EamSpoilageSettingsProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
		super(output, registries, PackOutput.Target.DATA_PACK, "spoilage", SpoilageSettings.CODEC);
	}

	@Override
	protected void configure(BiConsumer<Identifier, SpoilageSettings> provider, HolderLookup.Provider registries) {
		provider.accept(Eam.id("settings"), SpoilageSettings.DEFAULT);
	}

	@Override
	public String getName() {
		return "EAM spoilage settings";
	}
}
