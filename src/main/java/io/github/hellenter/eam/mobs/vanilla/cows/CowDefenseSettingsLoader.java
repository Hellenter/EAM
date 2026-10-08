package io.github.hellenter.eam.mobs.vanilla.cows;

import io.github.hellenter.eam.Eam;

import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.Map;

/** Reads {@code data/eam/mobs/cow_defense.json} on every datapack (re)load. */
final class CowDefenseSettingsLoader extends SimpleJsonResourceReloadListener<CowDefenseSettings> {
	static final Identifier ID = Eam.id("cow_defense_settings");
	static final Identifier SETTINGS = Eam.id("cow_defense");

	CowDefenseSettingsLoader() {
		super(CowDefenseSettings.CODEC, FileToIdConverter.json("mobs"));
	}

	@Override
	protected void apply(Map<Identifier, CowDefenseSettings> files, ResourceManager manager, ProfilerFiller profiler) {
		CowDefenseModule.setSettings(files.getOrDefault(SETTINGS, CowDefenseSettings.DEFAULT));
	}
}
