package io.github.hellenter.eam.mobs.entity.draugr;

import io.github.hellenter.eam.Eam;

import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.Map;

/** Reads {@code data/eam/mobs/draugr.json} on every datapack (re)load. */
final class DraugrSettingsLoader extends SimpleJsonResourceReloadListener<DraugrSettings> {
	static final Identifier ID = Eam.id("draugr_settings");
	static final Identifier SETTINGS = Eam.id("draugr");

	DraugrSettingsLoader() {
		super(DraugrSettings.CODEC, FileToIdConverter.json("mobs"));
	}

	@Override
	protected void apply(Map<Identifier, DraugrSettings> files, ResourceManager manager, ProfilerFiller profiler) {
		DraugrModule.setSettings(files.getOrDefault(SETTINGS, DraugrSettings.DEFAULT));
	}
}
