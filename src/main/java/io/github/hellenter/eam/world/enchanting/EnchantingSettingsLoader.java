package io.github.hellenter.eam.world.enchanting;

import io.github.hellenter.eam.Eam;

import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.Map;

/** Reads {@code data/eam/enchanting/costs.json} on every datapack (re)load. */
final class EnchantingSettingsLoader extends SimpleJsonResourceReloadListener<EnchantingSettings> {
	static final Identifier ID = Eam.id("enchanting_settings");
	static final Identifier SETTINGS = Eam.id("costs");

	EnchantingSettingsLoader() {
		super(EnchantingSettings.CODEC, FileToIdConverter.json("enchanting"));
	}

	@Override
	protected void apply(Map<Identifier, EnchantingSettings> files, ResourceManager manager, ProfilerFiller profiler) {
		EnchantingModule.setSettings(files.getOrDefault(SETTINGS, EnchantingSettings.DEFAULT));
	}
}
