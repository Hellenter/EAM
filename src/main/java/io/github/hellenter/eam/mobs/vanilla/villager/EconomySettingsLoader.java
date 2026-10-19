package io.github.hellenter.eam.mobs.vanilla.villager;

import io.github.hellenter.eam.Eam;

import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.Map;

/** Reads {@code data/eam/villagers/economy.json} on every datapack (re)load. */
final class EconomySettingsLoader extends SimpleJsonResourceReloadListener<EconomySettings> {
	static final Identifier ID = Eam.id("village_economy");
	static final Identifier SETTINGS = Eam.id("economy");

	EconomySettingsLoader() {
		super(EconomySettings.CODEC, FileToIdConverter.json("villagers"));
	}

	@Override
	protected void apply(Map<Identifier, EconomySettings> files, ResourceManager manager, ProfilerFiller profiler) {
		VillageEconomyModule.setSettings(files.getOrDefault(SETTINGS, EconomySettings.DEFAULT));
	}
}
