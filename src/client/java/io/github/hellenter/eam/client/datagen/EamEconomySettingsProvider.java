package io.github.hellenter.eam.client.datagen;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.mobs.vanilla.villager.EconomySettings;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricCodecDataProvider;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

/** Writes the default village economy balance to {@code data/eam/villagers/economy.json} so datapacks see what to override. */
public class EamEconomySettingsProvider extends FabricCodecDataProvider<EconomySettings> {
	public EamEconomySettingsProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
		super(output, registries, PackOutput.Target.DATA_PACK, "villagers", EconomySettings.CODEC);
	}

	@Override
	protected void configure(BiConsumer<Identifier, EconomySettings> provider, HolderLookup.Provider registries) {
		provider.accept(Eam.id("economy"), EconomySettings.DEFAULT);
	}

	@Override
	public String getName() {
		return "EAM village economy settings";
	}
}
