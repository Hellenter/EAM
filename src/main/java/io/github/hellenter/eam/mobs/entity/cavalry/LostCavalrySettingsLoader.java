package io.github.hellenter.eam.mobs.entity.cavalry;

import io.github.hellenter.eam.Eam;

import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.Map;

/** Reads {@code data/eam/mobs/lost_cavalry.json} on every datapack (re)load. */
final class LostCavalrySettingsLoader extends SimpleJsonResourceReloadListener<LostCavalrySettings> {
	static final Identifier ID = Eam.id("lost_cavalry_settings");
	static final Identifier SETTINGS = Eam.id("lost_cavalry");

	LostCavalrySettingsLoader() {
		super(LostCavalrySettings.CODEC, FileToIdConverter.json("mobs"));
	}

	@Override
	protected void apply(Map<Identifier, LostCavalrySettings> files, ResourceManager manager, ProfilerFiller profiler) {
		LostCavalryModule.setSettings(files.getOrDefault(SETTINGS, LostCavalrySettings.DEFAULT));
	}
}
