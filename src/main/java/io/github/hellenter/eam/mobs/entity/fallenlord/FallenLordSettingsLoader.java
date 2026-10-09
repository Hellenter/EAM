package io.github.hellenter.eam.mobs.entity.fallenlord;

import io.github.hellenter.eam.Eam;

import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.Map;

/** Reads {@code data/eam/mobs/fallen_lord.json} on every datapack (re)load. */
final class FallenLordSettingsLoader extends SimpleJsonResourceReloadListener<FallenLordSettings> {
	static final Identifier ID = Eam.id("fallen_lord_settings");
	static final Identifier SETTINGS = Eam.id("fallen_lord");

	FallenLordSettingsLoader() {
		super(FallenLordSettings.CODEC, FileToIdConverter.json("mobs"));
	}

	@Override
	protected void apply(Map<Identifier, FallenLordSettings> files, ResourceManager manager, ProfilerFiller profiler) {
		FallenLordModule.setSettings(files.getOrDefault(SETTINGS, FallenLordSettings.DEFAULT));
	}
}
