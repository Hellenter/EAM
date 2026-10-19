package io.github.hellenter.eam.client.datagen;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.content.oven.ClayOvenSettings;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricCodecDataProvider;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

/** Writes the default clay oven balance to {@code data/eam/clay_oven/settings.json} so datapacks see what to override. */
public class EamClayOvenSettingsProvider extends FabricCodecDataProvider<ClayOvenSettings> {
	public EamClayOvenSettingsProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
		super(output, registries, PackOutput.Target.DATA_PACK, "clay_oven", ClayOvenSettings.CODEC);
	}

	@Override
	protected void configure(BiConsumer<Identifier, ClayOvenSettings> provider, HolderLookup.Provider registries) {
		provider.accept(Eam.id("settings"), ClayOvenSettings.DEFAULT);
	}

	@Override
	public String getName() {
		return "EAM clay oven settings";
	}
}
