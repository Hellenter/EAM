package io.github.hellenter.eam.mobs.entity.knight;

import io.github.hellenter.eam.Eam;

import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.Map;

/** Reads {@code data/eam/mobs/skeleton_knight.json} on every datapack (re)load. */
final class SkeletonKnightSettingsLoader extends SimpleJsonResourceReloadListener<SkeletonKnightSettings> {
	static final Identifier ID = Eam.id("skeleton_knight_settings");
	static final Identifier SETTINGS = Eam.id("skeleton_knight");

	SkeletonKnightSettingsLoader() {
		super(SkeletonKnightSettings.CODEC, FileToIdConverter.json("mobs"));
	}

	@Override
	protected void apply(Map<Identifier, SkeletonKnightSettings> files, ResourceManager manager, ProfilerFiller profiler) {
		SkeletonKnightModule.setSettings(files.getOrDefault(SETTINGS, SkeletonKnightSettings.DEFAULT));
	}
}
