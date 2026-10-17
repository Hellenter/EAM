package io.github.hellenter.eam.mobs.vanilla.noise;

import io.github.hellenter.eam.Eam;

import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.Map;

/** Reads {@code data/eam/mobs/noise.json} on every datapack (re)load. */
final class NoiseSettingsLoader extends SimpleJsonResourceReloadListener<NoiseSettings> {
	static final Identifier ID = Eam.id("noise_settings");
	static final Identifier SETTINGS = Eam.id("noise");

	NoiseSettingsLoader() {
		super(NoiseSettings.CODEC, FileToIdConverter.json("mobs"));
	}

	@Override
	protected void apply(Map<Identifier, NoiseSettings> files, ResourceManager manager, ProfilerFiller profiler) {
		NoiseModule.setSettings(files.getOrDefault(SETTINGS, NoiseSettings.DEFAULT));
	}
}
