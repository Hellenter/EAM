package io.github.hellenter.eam.world.bloodmoon;

import io.github.hellenter.eam.Eam;

import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.Map;

/** Reads {@code data/eam/events/blood_moon.json} on every datapack (re)load. */
final class BloodMoonSettingsLoader extends SimpleJsonResourceReloadListener<BloodMoonSettings> {
	static final Identifier ID = Eam.id("blood_moon_settings");
	static final Identifier SETTINGS = Eam.id("blood_moon");

	BloodMoonSettingsLoader() {
		super(BloodMoonSettings.CODEC, FileToIdConverter.json("events"));
	}

	@Override
	protected void apply(Map<Identifier, BloodMoonSettings> files, ResourceManager manager, ProfilerFiller profiler) {
		BloodMoonModule.setSettings(files.getOrDefault(SETTINGS, BloodMoonSettings.DEFAULT));
	}
}
