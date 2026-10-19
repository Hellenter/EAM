package io.github.hellenter.eam.mobs.vanilla.villager;

import io.github.hellenter.eam.Eam;

import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.Map;

/** Reads {@code data/eam/villagers/donations.json} on every datapack (re)load. */
final class DonationSettingsLoader extends SimpleJsonResourceReloadListener<DonationSettings> {
	static final Identifier ID = Eam.id("village_donations");
	static final Identifier SETTINGS = Eam.id("donations");

	DonationSettingsLoader() {
		super(DonationSettings.CODEC, FileToIdConverter.json("villagers"));
	}

	@Override
	protected void apply(Map<Identifier, DonationSettings> files, ResourceManager manager, ProfilerFiller profiler) {
		VillageEconomyModule.setDonations(files.getOrDefault(SETTINGS, DonationSettings.DEFAULT));
	}
}
