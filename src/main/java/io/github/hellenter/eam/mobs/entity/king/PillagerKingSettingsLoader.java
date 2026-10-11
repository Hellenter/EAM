package io.github.hellenter.eam.mobs.entity.king;

import io.github.hellenter.eam.Eam;

import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.Map;

/** Reads {@code data/eam/mobs/pillager_king.json} on every datapack (re)load. */
final class PillagerKingSettingsLoader extends SimpleJsonResourceReloadListener<PillagerKingSettings> {
	static final Identifier ID = Eam.id("pillager_king_settings");
	static final Identifier SETTINGS = Eam.id("pillager_king");

	PillagerKingSettingsLoader() {
		super(PillagerKingSettings.CODEC, FileToIdConverter.json("mobs"));
	}

	@Override
	protected void apply(Map<Identifier, PillagerKingSettings> files, ResourceManager manager, ProfilerFiller profiler) {
		PillagerKingModule.setSettings(files.getOrDefault(SETTINGS, PillagerKingSettings.DEFAULT));
	}
}
