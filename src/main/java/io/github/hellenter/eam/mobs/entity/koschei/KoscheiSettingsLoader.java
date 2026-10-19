package io.github.hellenter.eam.mobs.entity.koschei;

import io.github.hellenter.eam.Eam;

import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.Map;

/** Reads {@code data/eam/mobs/koschei.json} on every datapack (re)load. */
final class KoscheiSettingsLoader extends SimpleJsonResourceReloadListener<KoscheiSettings> {
	static final Identifier ID = Eam.id("koschei_settings");
	static final Identifier SETTINGS = Eam.id("koschei");

	KoscheiSettingsLoader() {
		super(KoscheiSettings.CODEC, FileToIdConverter.json("mobs"));
	}

	@Override
	protected void apply(Map<Identifier, KoscheiSettings> files, ResourceManager manager, ProfilerFiller profiler) {
		KoscheiModule.setSettings(files.getOrDefault(SETTINGS, KoscheiSettings.DEFAULT));
	}
}
