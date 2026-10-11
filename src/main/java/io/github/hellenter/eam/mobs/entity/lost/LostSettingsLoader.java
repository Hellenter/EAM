package io.github.hellenter.eam.mobs.entity.lost;

import io.github.hellenter.eam.Eam;

import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.Map;

/** Reads {@code data/eam/mobs/lost.json} on every datapack (re)load. */
final class LostSettingsLoader extends SimpleJsonResourceReloadListener<LostSettings> {
	static final Identifier ID = Eam.id("lost_settings");
	static final Identifier SETTINGS = Eam.id("lost");

	LostSettingsLoader() {
		super(LostSettings.CODEC, FileToIdConverter.json("mobs"));
	}

	@Override
	protected void apply(Map<Identifier, LostSettings> files, ResourceManager manager, ProfilerFiller profiler) {
		LostModule.setSettings(files.getOrDefault(SETTINGS, LostSettings.DEFAULT));
	}
}
