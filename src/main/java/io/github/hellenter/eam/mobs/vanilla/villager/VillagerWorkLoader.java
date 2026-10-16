package io.github.hellenter.eam.mobs.vanilla.villager;

import io.github.hellenter.eam.Eam;

import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.Map;

/** Reads {@code data/<namespace>/villager_work/<profession>.json} on every datapack (re)load. */
final class VillagerWorkLoader extends SimpleJsonResourceReloadListener<VillagerWork> {
	static final Identifier ID = Eam.id("villager_work");

	VillagerWorkLoader() {
		super(VillagerWork.CODEC, FileToIdConverter.json("villager_work"));
	}

	@Override
	protected void apply(Map<Identifier, VillagerWork> files, ResourceManager manager, ProfilerFiller profiler) {
		VillagerGearModule.setWork(Map.copyOf(files));
	}
}
