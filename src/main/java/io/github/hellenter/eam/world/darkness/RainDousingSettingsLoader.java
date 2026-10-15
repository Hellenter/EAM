package io.github.hellenter.eam.world.darkness;

import io.github.hellenter.eam.Eam;

import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.Map;

/** Reads {@code data/eam/rain_dousing/settings.json} on every datapack (re)load. */
final class RainDousingSettingsLoader extends SimpleJsonResourceReloadListener<RainDousingSettings> {
	static final Identifier ID = Eam.id("rain_dousing_settings");
	static final Identifier SETTINGS = Eam.id("settings");

	RainDousingSettingsLoader() {
		super(RainDousingSettings.CODEC, FileToIdConverter.json("rain_dousing"));
	}

	@Override
	protected void apply(Map<Identifier, RainDousingSettings> files, ResourceManager manager, ProfilerFiller profiler) {
		RainDousing.setSettings(files.getOrDefault(SETTINGS, RainDousingSettings.DEFAULT));
	}
}
