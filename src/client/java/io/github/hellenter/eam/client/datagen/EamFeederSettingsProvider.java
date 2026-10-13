package io.github.hellenter.eam.client.datagen;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.content.feeder.FeederSettings;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricCodecDataProvider;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

/** Writes the default feeder and livestock balance to {@code data/eam/feeder/settings.json} so datapacks see what to override. */
public class EamFeederSettingsProvider extends FabricCodecDataProvider<FeederSettings> {
	public EamFeederSettingsProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
		super(output, registries, PackOutput.Target.DATA_PACK, "feeder", FeederSettings.CODEC);
	}

	@Override
	protected void configure(BiConsumer<Identifier, FeederSettings> provider, HolderLookup.Provider registries) {
		provider.accept(Eam.id("settings"), FeederSettings.DEFAULT);
	}

	@Override
	public String getName() {
		return "EAM feeder settings";
	}
}
