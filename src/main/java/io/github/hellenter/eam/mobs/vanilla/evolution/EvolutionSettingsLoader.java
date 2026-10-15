package io.github.hellenter.eam.mobs.vanilla.evolution;

import io.github.hellenter.eam.Eam;

import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.Map;

/** Reads {@code data/eam/mobs/evolution.json} on every datapack (re)load. */
final class EvolutionSettingsLoader extends SimpleJsonResourceReloadListener<EvolutionSettings> {
	static final Identifier ID = Eam.id("evolution_settings");
	static final Identifier SETTINGS = Eam.id("evolution");

	EvolutionSettingsLoader() {
		super(EvolutionSettings.CODEC, FileToIdConverter.json("mobs"));
	}

	@Override
	protected void apply(Map<Identifier, EvolutionSettings> files, ResourceManager manager, ProfilerFiller profiler) {
		MonsterEvolutionModule.setSettings(files.getOrDefault(SETTINGS, EvolutionSettings.DEFAULT));
	}
}
