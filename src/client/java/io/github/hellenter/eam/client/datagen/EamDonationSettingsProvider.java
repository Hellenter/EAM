package io.github.hellenter.eam.client.datagen;

import io.github.hellenter.eam.Eam;
import io.github.hellenter.eam.mobs.vanilla.villager.DonationSettings;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricCodecDataProvider;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

/** Writes the default worth of donated items to {@code data/eam/villagers/donations.json} so datapacks see what to override. */
public class EamDonationSettingsProvider extends FabricCodecDataProvider<DonationSettings> {
	public EamDonationSettingsProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
		super(output, registries, PackOutput.Target.DATA_PACK, "villagers", DonationSettings.CODEC);
	}

	@Override
	protected void configure(BiConsumer<Identifier, DonationSettings> provider, HolderLookup.Provider registries) {
		provider.accept(Eam.id("donations"), DonationSettings.DEFAULT);
	}

	@Override
	public String getName() {
		return "EAM village donation settings";
	}
}
