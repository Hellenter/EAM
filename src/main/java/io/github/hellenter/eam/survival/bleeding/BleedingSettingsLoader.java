package io.github.hellenter.eam.survival.bleeding;

import io.github.hellenter.eam.Eam;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;

import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.Map;

/** Reads {@code data/eam/survival/bleeding.json} on every datapack (re)load. */
final class BleedingSettingsLoader extends SimpleJsonResourceReloadListener<JsonElement> {
	static final Identifier ID = Eam.id("bleeding_settings");
	private static final Identifier FILE = Eam.id("bleeding");

	BleedingSettingsLoader() {
		super(ExtraCodecs.JSON, FileToIdConverter.json("survival"));
	}

	@Override
	protected void apply(Map<Identifier, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
		JsonElement json = files.get(FILE);
		BleedingModule.setSettings(json == null ? BleedingSettings.DEFAULT : BleedingSettings.CODEC.parse(JsonOps.INSTANCE, json)
				.resultOrPartial(error -> Eam.LOGGER.warn("Bad {}: {}", FILE, error))
				.orElse(BleedingSettings.DEFAULT));
	}
}
