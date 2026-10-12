package io.github.hellenter.eam.mobs.entity.yaga;

import io.github.hellenter.eam.Eam;

import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.Map;

/** Reads {@code data/eam/mobs/baba_yaga.json} on every datapack (re)load. */
final class BabaYagaSettingsLoader extends SimpleJsonResourceReloadListener<BabaYagaSettings> {
	static final Identifier ID = Eam.id("baba_yaga_settings");
	static final Identifier SETTINGS = Eam.id("baba_yaga");

	BabaYagaSettingsLoader() {
		super(BabaYagaSettings.CODEC, FileToIdConverter.json("mobs"));
	}

	@Override
	protected void apply(Map<Identifier, BabaYagaSettings> files, ResourceManager manager, ProfilerFiller profiler) {
		BabaYagaModule.setSettings(files.getOrDefault(SETTINGS, BabaYagaSettings.DEFAULT));
	}
}
