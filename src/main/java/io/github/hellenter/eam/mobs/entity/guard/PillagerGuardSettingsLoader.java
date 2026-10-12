package io.github.hellenter.eam.mobs.entity.guard;

import io.github.hellenter.eam.Eam;

import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.Map;

/** Reads {@code data/eam/mobs/pillager_guard.json} on every datapack (re)load. */
final class PillagerGuardSettingsLoader extends SimpleJsonResourceReloadListener<PillagerGuardSettings> {
	static final Identifier ID = Eam.id("pillager_guard_settings");
	static final Identifier SETTINGS = Eam.id("pillager_guard");

	PillagerGuardSettingsLoader() {
		super(PillagerGuardSettings.CODEC, FileToIdConverter.json("mobs"));
	}

	@Override
	protected void apply(Map<Identifier, PillagerGuardSettings> files, ResourceManager manager, ProfilerFiller profiler) {
		PillagerGuardModule.setSettings(files.getOrDefault(SETTINGS, PillagerGuardSettings.DEFAULT));
	}
}
