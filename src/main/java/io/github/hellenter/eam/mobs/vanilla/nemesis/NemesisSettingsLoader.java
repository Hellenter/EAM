package io.github.hellenter.eam.mobs.vanilla.nemesis;

import io.github.hellenter.eam.Eam;

import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.Map;

/** Reads {@code data/eam/mobs/nemesis.json} on every datapack (re)load. */
final class NemesisSettingsLoader extends SimpleJsonResourceReloadListener<NemesisSettings> {
	static final Identifier ID = Eam.id("nemesis_settings");
	static final Identifier SETTINGS = Eam.id("nemesis");

	NemesisSettingsLoader() {
		super(NemesisSettings.CODEC, FileToIdConverter.json("mobs"));
	}

	@Override
	protected void apply(Map<Identifier, NemesisSettings> files, ResourceManager manager, ProfilerFiller profiler) {
		NemesisModule.setSettings(files.getOrDefault(SETTINGS, NemesisSettings.DEFAULT));
	}
}
