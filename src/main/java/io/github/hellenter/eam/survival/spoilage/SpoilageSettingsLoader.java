package io.github.hellenter.eam.survival.spoilage;

import io.github.hellenter.eam.Eam;

import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.Map;

/** Reads {@code data/eam/spoilage/settings.json} on every datapack (re)load. */
final class SpoilageSettingsLoader extends SimpleJsonResourceReloadListener<SpoilageSettings> {
	static final Identifier ID = Eam.id("spoilage_settings");
	static final Identifier SETTINGS = Eam.id("settings");

	SpoilageSettingsLoader() {
		super(SpoilageSettings.CODEC, FileToIdConverter.json("spoilage"));
	}

	@Override
	protected void apply(Map<Identifier, SpoilageSettings> files, ResourceManager manager, ProfilerFiller profiler) {
		SpoilageModule.setSettings(files.getOrDefault(SETTINGS, SpoilageSettings.DEFAULT));
	}
}
