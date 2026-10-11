package io.github.hellenter.eam.mobs.entity.warlock;

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

/** Reads {@code data/eam/mobs/undead_warlock.json} on every datapack (re)load. */
final class UndeadWarlockSettingsLoader extends SimpleJsonResourceReloadListener<JsonElement> {
	static final Identifier ID = Eam.id("undead_warlock_settings");
	private static final Identifier FILE = Eam.id("undead_warlock");

	UndeadWarlockSettingsLoader() {
		super(ExtraCodecs.JSON, FileToIdConverter.json("mobs"));
	}

	@Override
	protected void apply(Map<Identifier, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
		JsonElement json = files.get(FILE);
		UndeadWarlockModule.setSettings(json == null ? UndeadWarlockSettings.DEFAULT : UndeadWarlockSettings.CODEC.parse(JsonOps.INSTANCE, json)
				.resultOrPartial(error -> Eam.LOGGER.warn("Bad {}: {}", FILE, error))
				.orElse(UndeadWarlockSettings.DEFAULT));
	}
}
