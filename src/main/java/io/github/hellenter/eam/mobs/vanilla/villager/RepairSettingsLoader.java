package io.github.hellenter.eam.mobs.vanilla.villager;

import io.github.hellenter.eam.Eam;

import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.Map;

/** Reads {@code data/eam/villagers/repair.json} on every datapack (re)load. */
final class RepairSettingsLoader extends SimpleJsonResourceReloadListener<RepairSettings> {
	static final Identifier ID = Eam.id("villager_repair");
	static final Identifier SETTINGS = Eam.id("repair");

	RepairSettingsLoader() {
		super(RepairSettings.CODEC, FileToIdConverter.json("villagers"));
	}

	@Override
	protected void apply(Map<Identifier, RepairSettings> files, ResourceManager manager, ProfilerFiller profiler) {
		VillagerRepairModule.setSettings(files.getOrDefault(SETTINGS, RepairSettings.DEFAULT));
	}
}
