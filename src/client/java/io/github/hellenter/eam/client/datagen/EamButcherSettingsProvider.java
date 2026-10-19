package io.github.hellenter.eam.client.datagen;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.mobs.vanilla.villager.ButcherSettings;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricCodecDataProvider;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

/** Writes the default butcher balance to {@code data/eam/villagers/butcher.json} so datapacks see what to override. */
public class EamButcherSettingsProvider extends FabricCodecDataProvider<ButcherSettings> {
	public EamButcherSettingsProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
		super(output, registries, PackOutput.Target.DATA_PACK, "villagers", ButcherSettings.CODEC);
	}

	@Override
	protected void configure(BiConsumer<Identifier, ButcherSettings> provider, HolderLookup.Provider registries) {
		provider.accept(Eam.id("butcher"), ButcherSettings.DEFAULT);
	}

	@Override
	public String getName() {
		return "EAM butcher settings";
	}
}
