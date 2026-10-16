package io.github.hellenter.eam.mobs.vanilla.villager;

import io.github.hellenter.eam.Eam;

import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.Map;

/** Reads {@code data/eam/villagers/cleric.json} on every datapack (re)load. */
final class ClericSettingsLoader extends SimpleJsonResourceReloadListener<ClericSettings> {
	static final Identifier ID = Eam.id("villager_cleric");
	static final Identifier SETTINGS = Eam.id("cleric");

	ClericSettingsLoader() {
		super(ClericSettings.CODEC, FileToIdConverter.json("villagers"));
	}

	@Override
	protected void apply(Map<Identifier, ClericSettings> files, ResourceManager manager, ProfilerFiller profiler) {
		VillagerClericModule.setSettings(files.getOrDefault(SETTINGS, ClericSettings.DEFAULT));
	}
}
