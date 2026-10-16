package io.github.hellenter.eam.client.datagen;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.mobs.vanilla.villager.ElderSettings;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricCodecDataProvider;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

/** Writes the default elder balance to {@code data/eam/villagers/elder.json} so datapacks see what to override. */
public class EamElderSettingsProvider extends FabricCodecDataProvider<ElderSettings> {
	public EamElderSettingsProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
		super(output, registries, PackOutput.Target.DATA_PACK, "villagers", ElderSettings.CODEC);
	}

	@Override
	protected void configure(BiConsumer<Identifier, ElderSettings> provider, HolderLookup.Provider registries) {
		provider.accept(Eam.id("elder"), ElderSettings.DEFAULT);
	}

	@Override
	public String getName() {
		return "EAM elder settings";
	}
}
