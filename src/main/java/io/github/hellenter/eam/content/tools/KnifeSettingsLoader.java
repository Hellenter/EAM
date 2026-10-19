package io.github.hellenter.eam.content.tools;

import io.github.hellenter.eam.Eam;

import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.Map;

/** Reads {@code data/eam/tools/knife.json} on every datapack (re)load. */
final class KnifeSettingsLoader extends SimpleJsonResourceReloadListener<KnifeSettings> {
	static final Identifier ID = Eam.id("knife_settings");
	static final Identifier SETTINGS = Eam.id("knife");

	KnifeSettingsLoader() {
		super(KnifeSettings.CODEC, FileToIdConverter.json("tools"));
	}

	@Override
	protected void apply(Map<Identifier, KnifeSettings> files, ResourceManager manager, ProfilerFiller profiler) {
		KnifeModule.setSettings(files.getOrDefault(SETTINGS, KnifeSettings.DEFAULT));
	}
}
