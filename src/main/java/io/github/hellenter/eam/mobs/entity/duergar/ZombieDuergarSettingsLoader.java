package io.github.hellenter.eam.mobs.entity.duergar;

import io.github.hellenter.eam.Eam;

import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.Map;

/** Reads {@code data/eam/mobs/zombie_duergar.json} on every datapack (re)load. */
final class ZombieDuergarSettingsLoader extends SimpleJsonResourceReloadListener<ZombieDuergarSettings> {
	static final Identifier ID = Eam.id("zombie_duergar_settings");
	static final Identifier SETTINGS = Eam.id("zombie_duergar");

	ZombieDuergarSettingsLoader() {
		super(ZombieDuergarSettings.CODEC, FileToIdConverter.json("mobs"));
	}

	@Override
	protected void apply(Map<Identifier, ZombieDuergarSettings> files, ResourceManager manager, ProfilerFiller profiler) {
		DuergarModule.setZombieSettings(files.getOrDefault(SETTINGS, ZombieDuergarSettings.DEFAULT));
	}
}
