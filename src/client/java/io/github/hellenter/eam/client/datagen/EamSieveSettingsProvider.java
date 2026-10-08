package io.github.hellenter.eam.client.datagen;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.content.sieve.SieveSettings;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricCodecDataProvider;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

/** Writes the default sieve balance to {@code data/eam/sieve/settings.json} so datapacks see what to override. */
public class EamSieveSettingsProvider extends FabricCodecDataProvider<SieveSettings> {
	public EamSieveSettingsProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
		super(output, registries, PackOutput.Target.DATA_PACK, "sieve", SieveSettings.CODEC);
	}

	@Override
	protected void configure(BiConsumer<Identifier, SieveSettings> provider, HolderLookup.Provider registries) {
		provider.accept(Eam.id("settings"), SieveSettings.DEFAULT);
	}

	@Override
	public String getName() {
		return "EAM sieve settings";
	}
}
