package io.github.hellenter.eam.mobs.entity.bat;

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

/** Reads {@code data/eam/mobs/vampire_bat.json} on every datapack (re)load. */
final class VampireBatSettingsLoader extends SimpleJsonResourceReloadListener<JsonElement> {
	static final Identifier ID = Eam.id("vampire_bat_settings");
	private static final Identifier FILE = Eam.id("vampire_bat");

	VampireBatSettingsLoader() {
		super(ExtraCodecs.JSON, FileToIdConverter.json("mobs"));
	}

	@Override
	protected void apply(Map<Identifier, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
		JsonElement json = files.get(FILE);
		VampireBatModule.setSettings(json == null ? VampireBatSettings.DEFAULT : VampireBatSettings.CODEC.parse(JsonOps.INSTANCE, json)
				.resultOrPartial(error -> Eam.LOGGER.warn("Bad {}: {}", FILE, error))
				.orElse(VampireBatSettings.DEFAULT));
	}
}
