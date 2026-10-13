package io.github.hellenter.eam.content.feeder;

import io.github.hellenter.eam.Eam;

import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.Map;

/** Reads {@code data/eam/feeder/settings.json} on every datapack (re)load. */
final class FeederSettingsLoader extends SimpleJsonResourceReloadListener<FeederSettings> {
	static final Identifier ID = Eam.id("feeder_settings");
	static final Identifier SETTINGS = Eam.id("settings");

	FeederSettingsLoader() {
		super(FeederSettings.CODEC, FileToIdConverter.json("feeder"));
	}

	@Override
	protected void apply(Map<Identifier, FeederSettings> files, ResourceManager manager, ProfilerFiller profiler) {
		FeederModule.setSettings(files.getOrDefault(SETTINGS, FeederSettings.DEFAULT));
	}
}
