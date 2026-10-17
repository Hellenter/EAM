package io.github.hellenter.eam.content.dryer;

import io.github.hellenter.eam.Eam;

import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.Map;

/** Reads {@code data/eam/food_dryer/settings.json} on every datapack (re)load. */
final class DryerSettingsLoader extends SimpleJsonResourceReloadListener<DryerSettings> {
	static final Identifier ID = Eam.id("food_dryer_settings");
	static final Identifier SETTINGS = Eam.id("settings");

	DryerSettingsLoader() {
		super(DryerSettings.CODEC, FileToIdConverter.json("food_dryer"));
	}

	@Override
	protected void apply(Map<Identifier, DryerSettings> files, ResourceManager manager, ProfilerFiller profiler) {
		DryerModule.setSettings(files.getOrDefault(SETTINGS, DryerSettings.DEFAULT));
	}
}
