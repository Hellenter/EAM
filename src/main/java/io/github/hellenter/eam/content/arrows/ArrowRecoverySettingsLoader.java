package io.github.hellenter.eam.content.arrows;

import io.github.hellenter.eam.Eam;

import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.Map;

/** Reads {@code data/eam/arrows/recovery.json} on every datapack (re)load. */
final class ArrowRecoverySettingsLoader extends SimpleJsonResourceReloadListener<ArrowRecoverySettings> {
	static final Identifier ID = Eam.id("arrow_recovery_settings");
	static final Identifier SETTINGS = Eam.id("recovery");

	ArrowRecoverySettingsLoader() {
		super(ArrowRecoverySettings.CODEC, FileToIdConverter.json("arrows"));
	}

	@Override
	protected void apply(Map<Identifier, ArrowRecoverySettings> files, ResourceManager manager, ProfilerFiller profiler) {
		ArrowRecoveryModule.setSettings(files.getOrDefault(SETTINGS, ArrowRecoverySettings.DEFAULT));
	}
}
