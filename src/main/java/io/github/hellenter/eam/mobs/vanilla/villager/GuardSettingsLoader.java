package io.github.hellenter.eam.mobs.vanilla.villager;

import io.github.hellenter.eam.Eam;

import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.Map;

/** Reads {@code data/eam/villagers/guards.json} on every datapack (re)load. */
final class GuardSettingsLoader extends SimpleJsonResourceReloadListener<GuardSettings> {
	static final Identifier ID = Eam.id("villager_guards");
	static final Identifier SETTINGS = Eam.id("guards");

	GuardSettingsLoader() {
		super(GuardSettings.CODEC, FileToIdConverter.json("villagers"));
	}

	@Override
	protected void apply(Map<Identifier, GuardSettings> files, ResourceManager manager, ProfilerFiller profiler) {
		VillagerGuardModule.setSettings(files.getOrDefault(SETTINGS, GuardSettings.DEFAULT));
	}
}
