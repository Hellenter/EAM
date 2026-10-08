package io.github.hellenter.eam.world.crafting;

import io.github.hellenter.eam.Eam;

import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.Map;

/** Reads {@code data/eam/crafting/times.json} on every datapack (re)load. */
final class CraftTimeRulesLoader extends SimpleJsonResourceReloadListener<CraftTimeRules> {
	static final Identifier ID = Eam.id("craft_times");
	static final Identifier TIMES = Eam.id("times");

	CraftTimeRulesLoader() {
		super(CraftTimeRules.CODEC, FileToIdConverter.json("crafting"));
	}

	@Override
	protected void apply(Map<Identifier, CraftTimeRules> files, ResourceManager manager, ProfilerFiller profiler) {
		CraftingModule.setRules(files.getOrDefault(TIMES, CraftTimeRules.DEFAULT));
	}
}
