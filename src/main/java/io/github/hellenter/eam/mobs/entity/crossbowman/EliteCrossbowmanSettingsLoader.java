package io.github.hellenter.eam.mobs.entity.crossbowman;

import io.github.hellenter.eam.Eam;

import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.Map;

/** Reads {@code data/eam/mobs/elite_crossbowman.json} on every datapack (re)load. */
final class EliteCrossbowmanSettingsLoader extends SimpleJsonResourceReloadListener<EliteCrossbowmanSettings> {
	static final Identifier ID = Eam.id("elite_crossbowman_settings");
	static final Identifier SETTINGS = Eam.id("elite_crossbowman");

	EliteCrossbowmanSettingsLoader() {
		super(EliteCrossbowmanSettings.CODEC, FileToIdConverter.json("mobs"));
	}

	@Override
	protected void apply(Map<Identifier, EliteCrossbowmanSettings> files, ResourceManager manager, ProfilerFiller profiler) {
		EliteCrossbowmanModule.setSettings(files.getOrDefault(SETTINGS, EliteCrossbowmanSettings.DEFAULT));
	}
}
