package io.github.hellenter.eam.world.bluemoon;

import io.github.hellenter.eam.Eam;

import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.Map;

/** Reads {@code data/eam/events/blue_moon.json} on every datapack (re)load. */
final class BlueMoonSettingsLoader extends SimpleJsonResourceReloadListener<BlueMoonSettings> {
	static final Identifier ID = Eam.id("blue_moon_settings");
	static final Identifier SETTINGS = Eam.id("blue_moon");

	BlueMoonSettingsLoader() {
		super(BlueMoonSettings.CODEC, FileToIdConverter.json("events"));
	}

	@Override
	protected void apply(Map<Identifier, BlueMoonSettings> files, ResourceManager manager, ProfilerFiller profiler) {
		BlueMoonModule.setSettings(files.getOrDefault(SETTINGS, BlueMoonSettings.DEFAULT));
	}
}
