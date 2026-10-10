package io.github.hellenter.eam.mobs.entity.wight;

import io.github.hellenter.eam.Eam;

import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.Map;

/** Reads {@code data/eam/mobs/wight.json} on every datapack (re)load. */
final class WightSettingsLoader extends SimpleJsonResourceReloadListener<WightSettings> {
	static final Identifier ID = Eam.id("wight_settings");
	static final Identifier SETTINGS = Eam.id("wight");

	WightSettingsLoader() {
		super(WightSettings.CODEC, FileToIdConverter.json("mobs"));
	}

	@Override
	protected void apply(Map<Identifier, WightSettings> files, ResourceManager manager, ProfilerFiller profiler) {
		WightModule.setSettings(files.getOrDefault(SETTINGS, WightSettings.DEFAULT));
	}
}
