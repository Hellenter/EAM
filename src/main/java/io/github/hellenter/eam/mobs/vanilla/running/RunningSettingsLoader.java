package io.github.hellenter.eam.mobs.vanilla.running;

import io.github.hellenter.eam.Eam;

import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.Map;

/** Reads {@code data/eam/mobs/running.json} on every datapack (re)load. */
final class RunningSettingsLoader extends SimpleJsonResourceReloadListener<RunningSettings> {
	static final Identifier ID = Eam.id("running_settings");
	static final Identifier SETTINGS = Eam.id("running");

	RunningSettingsLoader() {
		super(RunningSettings.CODEC, FileToIdConverter.json("mobs"));
	}

	@Override
	protected void apply(Map<Identifier, RunningSettings> files, ResourceManager manager, ProfilerFiller profiler) {
		RunningModule.setSettings(files.getOrDefault(SETTINGS, RunningSettings.DEFAULT));
	}
}
