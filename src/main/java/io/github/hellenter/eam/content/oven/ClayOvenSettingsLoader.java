package io.github.hellenter.eam.content.oven;

import io.github.hellenter.eam.Eam;

import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.Map;

/** Reads {@code data/eam/clay_oven/settings.json} on every datapack (re)load. */
final class ClayOvenSettingsLoader extends SimpleJsonResourceReloadListener<ClayOvenSettings> {
	static final Identifier ID = Eam.id("clay_oven_settings");
	static final Identifier SETTINGS = Eam.id("settings");

	ClayOvenSettingsLoader() {
		super(ClayOvenSettings.CODEC, FileToIdConverter.json("clay_oven"));
	}

	@Override
	protected void apply(Map<Identifier, ClayOvenSettings> files, ResourceManager manager, ProfilerFiller profiler) {
		ClayOvenModule.setSettings(files.getOrDefault(SETTINGS, ClayOvenSettings.DEFAULT));
	}
}
