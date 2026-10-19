package io.github.hellenter.eam.mobs.vanilla.villager;

import io.github.hellenter.eam.Eam;

import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.Map;

/** Reads {@code data/eam/villagers/alarm.json} on every datapack (re)load. */
final class AlarmSettingsLoader extends SimpleJsonResourceReloadListener<AlarmSettings> {
	static final Identifier ID = Eam.id("villager_alarm");
	static final Identifier SETTINGS = Eam.id("alarm");

	AlarmSettingsLoader() {
		super(AlarmSettings.CODEC, FileToIdConverter.json("villagers"));
	}

	@Override
	protected void apply(Map<Identifier, AlarmSettings> files, ResourceManager manager, ProfilerFiller profiler) {
		VillagerAlarmModule.setSettings(files.getOrDefault(SETTINGS, AlarmSettings.DEFAULT));
	}
}
