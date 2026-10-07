package io.github.hellenter.eam.content.campfire;

import io.github.hellenter.eam.Eam;

import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.Map;

/** Reads {@code data/eam/camp_fire/settings.json} on every datapack (re)load. */
final class CampFireSettingsLoader extends SimpleJsonResourceReloadListener<CampFireSettings> {
	static final Identifier ID = Eam.id("camp_fire_settings");
	static final Identifier SETTINGS = Eam.id("settings");

	CampFireSettingsLoader() {
		super(CampFireSettings.CODEC, FileToIdConverter.json("camp_fire"));
	}

	@Override
	protected void apply(Map<Identifier, CampFireSettings> files, ResourceManager manager, ProfilerFiller profiler) {
		CampFireModule.setSettings(files.getOrDefault(SETTINGS, CampFireSettings.DEFAULT));
	}
}
