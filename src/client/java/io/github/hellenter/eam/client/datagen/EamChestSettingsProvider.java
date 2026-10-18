package io.github.hellenter.eam.client.datagen;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.mobs.vanilla.villager.ChestSettings;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricCodecDataProvider;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

/** Writes the default villager chest balance to {@code data/eam/villagers/chests.json} so datapacks see what to override. */
public class EamChestSettingsProvider extends FabricCodecDataProvider<ChestSettings> {
	public EamChestSettingsProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
		super(output, registries, PackOutput.Target.DATA_PACK, "villagers", ChestSettings.CODEC);
	}

	@Override
	protected void configure(BiConsumer<Identifier, ChestSettings> provider, HolderLookup.Provider registries) {
		provider.accept(Eam.id("chests"), ChestSettings.DEFAULT);
	}

	@Override
	public String getName() {
		return "EAM villager chest settings";
	}
}
