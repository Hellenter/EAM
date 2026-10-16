package io.github.hellenter.eam.mobs.vanilla.villager;

import io.github.hellenter.eam.Eam;

import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.Map;

/** Reads {@code data/eam/villagers/butcher.json} on every datapack (re)load. */
final class ButcherSettingsLoader extends SimpleJsonResourceReloadListener<ButcherSettings> {
	static final Identifier ID = Eam.id("villager_butcher");
	static final Identifier SETTINGS = Eam.id("butcher");

	ButcherSettingsLoader() {
		super(ButcherSettings.CODEC, FileToIdConverter.json("villagers"));
	}

	@Override
	protected void apply(Map<Identifier, ButcherSettings> files, ResourceManager manager, ProfilerFiller profiler) {
		VillagerButcherModule.setSettings(files.getOrDefault(SETTINGS, ButcherSettings.DEFAULT));
	}
}
