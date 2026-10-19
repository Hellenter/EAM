package io.github.hellenter.eam.client.datagen;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.mobs.vanilla.villager.FoodSettings;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricCodecDataProvider;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

/** Writes the default villager food balance to {@code data/eam/villagers/food.json} so datapacks see what to override. */
public class EamFoodSettingsProvider extends FabricCodecDataProvider<FoodSettings> {
	public EamFoodSettingsProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
		super(output, registries, PackOutput.Target.DATA_PACK, "villagers", FoodSettings.CODEC);
	}

	@Override
	protected void configure(BiConsumer<Identifier, FoodSettings> provider, HolderLookup.Provider registries) {
		provider.accept(Eam.id("food"), FoodSettings.DEFAULT);
	}

	@Override
	public String getName() {
		return "EAM villager food settings";
	}
}
