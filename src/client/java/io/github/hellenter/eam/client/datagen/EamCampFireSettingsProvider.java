package io.github.hellenter.eam.client.datagen;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.content.campfire.CampFireSettings;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricCodecDataProvider;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

/** Writes the default camp fire balance to {@code data/eam/camp_fire/settings.json} so datapacks see what to override. */
public class EamCampFireSettingsProvider extends FabricCodecDataProvider<CampFireSettings> {
	public EamCampFireSettingsProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
		super(output, registries, PackOutput.Target.DATA_PACK, "camp_fire", CampFireSettings.CODEC);
	}

	@Override
	protected void configure(BiConsumer<Identifier, CampFireSettings> provider, HolderLookup.Provider registries) {
		provider.accept(Eam.id("settings"), CampFireSettings.DEFAULT);
	}

	@Override
	public String getName() {
		return "EAM camp fire settings";
	}
}
