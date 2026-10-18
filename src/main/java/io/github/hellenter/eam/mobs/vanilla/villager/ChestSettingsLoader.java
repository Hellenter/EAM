package io.github.hellenter.eam.mobs.vanilla.villager;

import io.github.hellenter.eam.Eam;

import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.Map;

/** Reads {@code data/eam/villagers/chests.json} on every datapack (re)load. */
final class ChestSettingsLoader extends SimpleJsonResourceReloadListener<ChestSettings> {
	static final Identifier ID = Eam.id("villager_chests");
	static final Identifier SETTINGS = Eam.id("chests");

	ChestSettingsLoader() {
		super(ChestSettings.CODEC, FileToIdConverter.json("villagers"));
	}

	@Override
	protected void apply(Map<Identifier, ChestSettings> files, ResourceManager manager, ProfilerFiller profiler) {
		VillagerChestModule.setSettings(files.getOrDefault(SETTINGS, ChestSettings.DEFAULT));
	}
}
