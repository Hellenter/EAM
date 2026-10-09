package io.github.hellenter.eam.survival.sleep;

import io.github.hellenter.eam.Eam;

import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.Map;

/** Reads {@code data/eam/sleep/safe_room.json} on every datapack (re)load. */
final class SafeRoomSettingsLoader extends SimpleJsonResourceReloadListener<SafeRoomSettings> {
	static final Identifier ID = Eam.id("safe_room_settings");
	static final Identifier SETTINGS = Eam.id("safe_room");

	SafeRoomSettingsLoader() {
		super(SafeRoomSettings.CODEC, FileToIdConverter.json("sleep"));
	}

	@Override
	protected void apply(Map<Identifier, SafeRoomSettings> files, ResourceManager manager, ProfilerFiller profiler) {
		SafeRoomModule.setSettings(files.getOrDefault(SETTINGS, SafeRoomSettings.DEFAULT));
	}
}
