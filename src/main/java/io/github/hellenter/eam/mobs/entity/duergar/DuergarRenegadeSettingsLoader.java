package io.github.hellenter.eam.mobs.entity.duergar;

import io.github.hellenter.eam.Eam;

import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.Map;

/** Reads {@code data/eam/mobs/duergar_renegade.json} on every datapack (re)load. */
final class DuergarRenegadeSettingsLoader extends SimpleJsonResourceReloadListener<DuergarRenegadeSettings> {
	static final Identifier ID = Eam.id("duergar_renegade_settings");
	static final Identifier SETTINGS = Eam.id("duergar_renegade");

	DuergarRenegadeSettingsLoader() {
		super(DuergarRenegadeSettings.CODEC, FileToIdConverter.json("mobs"));
	}

	@Override
	protected void apply(Map<Identifier, DuergarRenegadeSettings> files, ResourceManager manager, ProfilerFiller profiler) {
		DuergarModule.setRenegadeSettings(files.getOrDefault(SETTINGS, DuergarRenegadeSettings.DEFAULT));
	}
}
