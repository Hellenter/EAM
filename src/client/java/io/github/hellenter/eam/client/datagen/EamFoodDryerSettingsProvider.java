package io.github.hellenter.eam.client.datagen;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.content.dryer.DryerSettings;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricCodecDataProvider;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

/** Writes the default food dryer balance to {@code data/eam/food_dryer/settings.json} so datapacks see what to override. */
public class EamFoodDryerSettingsProvider extends FabricCodecDataProvider<DryerSettings> {
	public EamFoodDryerSettingsProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
		super(output, registries, PackOutput.Target.DATA_PACK, "food_dryer", DryerSettings.CODEC);
	}

	@Override
	protected void configure(BiConsumer<Identifier, DryerSettings> provider, HolderLookup.Provider registries) {
		provider.accept(Eam.id("settings"), DryerSettings.DEFAULT);
	}

	@Override
	public String getName() {
		return "EAM food dryer settings";
	}
}
