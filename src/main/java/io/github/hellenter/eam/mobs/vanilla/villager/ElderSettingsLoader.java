package io.github.hellenter.eam.mobs.vanilla.villager;

import io.github.hellenter.eam.Eam;

import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.Map;

/** Reads {@code data/eam/villagers/elder.json} on every datapack (re)load. */
final class ElderSettingsLoader extends SimpleJsonResourceReloadListener<ElderSettings> {
	static final Identifier ID = Eam.id("villager_elder");
	static final Identifier SETTINGS = Eam.id("elder");

	ElderSettingsLoader() {
		super(ElderSettings.CODEC, FileToIdConverter.json("villagers"));
	}

	@Override
	protected void apply(Map<Identifier, ElderSettings> files, ResourceManager manager, ProfilerFiller profiler) {
		VillagerElderModule.setSettings(files.getOrDefault(SETTINGS, ElderSettings.DEFAULT));
	}
}
