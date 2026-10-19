package io.github.hellenter.eam.mobs.vanilla.raid;

import io.github.hellenter.eam.Eam;

import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.Map;

/** Reads {@code data/eam/events/village_raids.json} on every datapack (re)load. */
final class VillageRaidsSettingsLoader extends SimpleJsonResourceReloadListener<VillageRaidsSettings> {
	static final Identifier ID = Eam.id("village_raids_settings");
	static final Identifier SETTINGS = Eam.id("village_raids");

	VillageRaidsSettingsLoader() {
		super(VillageRaidsSettings.CODEC, FileToIdConverter.json("events"));
	}

	@Override
	protected void apply(Map<Identifier, VillageRaidsSettings> files, ResourceManager manager, ProfilerFiller profiler) {
		VillageRaidsModule.setSettings(files.getOrDefault(SETTINGS, VillageRaidsSettings.DEFAULT));
	}
}
