package io.github.hellenter.eam.mobs.entity.lurker;

import io.github.hellenter.eam.Eam;

import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.Map;

/** Reads {@code data/eam/mobs/swamp_lurker.json} on every datapack (re)load. */
final class SwampLurkerSettingsLoader extends SimpleJsonResourceReloadListener<SwampLurkerSettings> {
	static final Identifier ID = Eam.id("swamp_lurker_settings");
	static final Identifier SETTINGS = Eam.id("swamp_lurker");

	SwampLurkerSettingsLoader() {
		super(SwampLurkerSettings.CODEC, FileToIdConverter.json("mobs"));
	}

	@Override
	protected void apply(Map<Identifier, SwampLurkerSettings> files, ResourceManager manager, ProfilerFiller profiler) {
		SwampLurkerModule.setSettings(files.getOrDefault(SETTINGS, SwampLurkerSettings.DEFAULT));
	}
}
