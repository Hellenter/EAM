package io.github.hellenter.eam.mobs.entity.shambler;

import io.github.hellenter.eam.Eam;

import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.Map;

/** Reads {@code data/eam/mobs/mushroom_shambler.json} on every datapack (re)load. */
final class ShamblerSettingsLoader extends SimpleJsonResourceReloadListener<ShamblerSettings> {
	static final Identifier ID = Eam.id("mushroom_shambler_settings");
	static final Identifier SETTINGS = Eam.id("mushroom_shambler");

	ShamblerSettingsLoader() {
		super(ShamblerSettings.CODEC, FileToIdConverter.json("mobs"));
	}

	@Override
	protected void apply(Map<Identifier, ShamblerSettings> files, ResourceManager manager, ProfilerFiller profiler) {
		ShamblerModule.setSettings(files.getOrDefault(SETTINGS, ShamblerSettings.DEFAULT));
	}
}
