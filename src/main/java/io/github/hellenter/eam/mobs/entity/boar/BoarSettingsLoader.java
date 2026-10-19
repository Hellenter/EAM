package io.github.hellenter.eam.mobs.entity.boar;

import io.github.hellenter.eam.Eam;

import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.Map;

/** Reads {@code data/eam/mobs/boar.json} on every datapack (re)load. */
final class BoarSettingsLoader extends SimpleJsonResourceReloadListener<BoarSettings> {
	static final Identifier ID = Eam.id("boar_settings");
	static final Identifier SETTINGS = Eam.id("boar");

	BoarSettingsLoader() {
		super(BoarSettings.CODEC, FileToIdConverter.json("mobs"));
	}

	@Override
	protected void apply(Map<Identifier, BoarSettings> files, ResourceManager manager, ProfilerFiller profiler) {
		BoarModule.setSettings(files.getOrDefault(SETTINGS, BoarSettings.DEFAULT));
	}
}
