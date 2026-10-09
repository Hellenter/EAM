package io.github.hellenter.eam.world.farming;

import io.github.hellenter.eam.Eam;

import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.Map;

/** Reads {@code data/eam/farming/growth.json} on every datapack (re)load. */
final class FarmingSettingsLoader extends SimpleJsonResourceReloadListener<FarmingSettings> {
	static final Identifier ID = Eam.id("farming_settings");
	static final Identifier SETTINGS = Eam.id("growth");

	FarmingSettingsLoader() {
		super(FarmingSettings.CODEC, FileToIdConverter.json("farming"));
	}

	@Override
	protected void apply(Map<Identifier, FarmingSettings> files, ResourceManager manager, ProfilerFiller profiler) {
		FarmingModule.setSettings(files.getOrDefault(SETTINGS, FarmingSettings.DEFAULT));
	}
}
