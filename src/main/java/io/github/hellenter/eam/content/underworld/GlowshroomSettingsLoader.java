package io.github.hellenter.eam.content.underworld;

import io.github.hellenter.eam.Eam;

import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.Map;

/** Reads {@code data/eam/glowshroom/settings.json} on every datapack (re)load. */
final class GlowshroomSettingsLoader extends SimpleJsonResourceReloadListener<GlowshroomSettings> {
	static final Identifier ID = Eam.id("glowshroom_settings");
	static final Identifier SETTINGS = Eam.id("settings");

	GlowshroomSettingsLoader() {
		super(GlowshroomSettings.CODEC, FileToIdConverter.json("glowshroom"));
	}

	@Override
	protected void apply(Map<Identifier, GlowshroomSettings> files, ResourceManager manager, ProfilerFiller profiler) {
		GlowshroomModule.setSettings(files.getOrDefault(SETTINGS, GlowshroomSettings.DEFAULT));
	}
}
