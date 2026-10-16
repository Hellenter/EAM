package io.github.hellenter.eam.client.datagen;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.mobs.vanilla.villager.ClericSettings;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricCodecDataProvider;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

/** Writes the default cleric balance to {@code data/eam/villagers/cleric.json} so datapacks see what to override. */
public class EamClericSettingsProvider extends FabricCodecDataProvider<ClericSettings> {
	public EamClericSettingsProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
		super(output, registries, PackOutput.Target.DATA_PACK, "villagers", ClericSettings.CODEC);
	}

	@Override
	protected void configure(BiConsumer<Identifier, ClericSettings> provider, HolderLookup.Provider registries) {
		provider.accept(Eam.id("cleric"), ClericSettings.DEFAULT);
	}

	@Override
	public String getName() {
		return "EAM cleric settings";
	}
}
