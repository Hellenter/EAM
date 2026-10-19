package io.github.hellenter.eam.mobs.entity.leshy;

import io.github.hellenter.eam.Eam;

import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.Map;

/** Reads {@code data/eam/mobs/leshy.json} on every datapack (re)load. */
final class LeshySettingsLoader extends SimpleJsonResourceReloadListener<LeshySettings> {
	static final Identifier ID = Eam.id("leshy_settings");
	static final Identifier SETTINGS = Eam.id("leshy");

	LeshySettingsLoader() {
		super(LeshySettings.CODEC, FileToIdConverter.json("mobs"));
	}

	@Override
	protected void apply(Map<Identifier, LeshySettings> files, ResourceManager manager, ProfilerFiller profiler) {
		LeshyModule.setSettings(files.getOrDefault(SETTINGS, LeshySettings.DEFAULT));
	}
}
