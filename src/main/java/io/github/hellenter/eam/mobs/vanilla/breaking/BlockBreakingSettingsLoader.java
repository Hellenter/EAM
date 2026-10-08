package io.github.hellenter.eam.mobs.vanilla.breaking;

import io.github.hellenter.eam.Eam;

import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.Map;

/** Reads {@code data/eam/mobs/block_breaking.json} on every datapack (re)load. */
final class BlockBreakingSettingsLoader extends SimpleJsonResourceReloadListener<BlockBreakingSettings> {
	static final Identifier ID = Eam.id("block_breaking_settings");
	static final Identifier SETTINGS = Eam.id("block_breaking");

	BlockBreakingSettingsLoader() {
		super(BlockBreakingSettings.CODEC, FileToIdConverter.json("mobs"));
	}

	@Override
	protected void apply(Map<Identifier, BlockBreakingSettings> files, ResourceManager manager, ProfilerFiller profiler) {
		BlockBreakingModule.setSettings(files.getOrDefault(SETTINGS, BlockBreakingSettings.DEFAULT));
	}
}
