package io.github.hellenter.eam.survival.thirst;

import io.github.hellenter.eam.Eam;

import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.Map;

/** Reads {@code data/eam/thirst/settings.json} on every datapack (re)load. */
final class ThirstSettingsLoader extends SimpleJsonResourceReloadListener<ThirstSettings> {
	static final Identifier ID = Eam.id("thirst_settings");
	static final Identifier SETTINGS = Eam.id("settings");

	ThirstSettingsLoader() {
		super(ThirstSettings.CODEC, FileToIdConverter.json("thirst"));
	}

	@Override
	protected void apply(Map<Identifier, ThirstSettings> files, ResourceManager manager, ProfilerFiller profiler) {
		ThirstModule.setSettings(files.getOrDefault(SETTINGS, ThirstSettings.DEFAULT));
	}
}
