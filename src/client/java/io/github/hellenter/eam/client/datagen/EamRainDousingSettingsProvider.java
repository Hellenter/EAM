package io.github.hellenter.eam.client.datagen;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.world.darkness.RainDousingSettings;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricCodecDataProvider;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

/** Writes the default rain dousing chances to {@code data/eam/rain_dousing/settings.json}. */
public class EamRainDousingSettingsProvider extends FabricCodecDataProvider<RainDousingSettings> {
	public EamRainDousingSettingsProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
		super(output, registries, PackOutput.Target.DATA_PACK, "rain_dousing", RainDousingSettings.CODEC);
	}

	@Override
	protected void configure(BiConsumer<Identifier, RainDousingSettings> provider, HolderLookup.Provider registries) {
		provider.accept(Eam.id("settings"), RainDousingSettings.DEFAULT);
	}

	@Override
	public String getName() {
		return "EAM rain dousing settings";
	}
}
