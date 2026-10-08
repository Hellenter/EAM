package io.github.hellenter.eam.content.sieve;

import io.github.hellenter.eam.Eam;

import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.Map;

/** Reads {@code data/eam/sieve/settings.json} on every datapack (re)load. */
final class SieveSettingsLoader extends SimpleJsonResourceReloadListener<SieveSettings> {
	static final Identifier ID = Eam.id("sieve_settings");
	static final Identifier SETTINGS = Eam.id("settings");

	SieveSettingsLoader() {
		super(SieveSettings.CODEC, FileToIdConverter.json("sieve"));
	}

	@Override
	protected void apply(Map<Identifier, SieveSettings> files, ResourceManager manager, ProfilerFiller profiler) {
		SieveModule.setSettings(files.getOrDefault(SETTINGS, SieveSettings.DEFAULT));
	}
}
