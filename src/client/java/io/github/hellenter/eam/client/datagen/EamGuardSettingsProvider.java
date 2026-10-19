package io.github.hellenter.eam.client.datagen;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.mobs.vanilla.villager.GuardSettings;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricCodecDataProvider;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

/** Writes the default villager guard balance to {@code data/eam/villagers/guards.json} so datapacks see what to override. */
public class EamGuardSettingsProvider extends FabricCodecDataProvider<GuardSettings> {
	public EamGuardSettingsProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
		super(output, registries, PackOutput.Target.DATA_PACK, "villagers", GuardSettings.CODEC);
	}

	@Override
	protected void configure(BiConsumer<Identifier, GuardSettings> provider, HolderLookup.Provider registries) {
		provider.accept(Eam.id("guards"), GuardSettings.DEFAULT);
	}

	@Override
	public String getName() {
		return "EAM villager guard settings";
	}
}
