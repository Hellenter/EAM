package io.github.hellenter.eam.mobs.entity.spider;

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

/** Reads {@code data/eam/mobs/wood_spider.json} and {@code black_widow.json} on every datapack (re)load. */
final class SpiderSettingsLoader extends SimpleJsonResourceReloadListener<JsonElement> {
	static final Identifier ID = Eam.id("spider_settings");
	static final Identifier WOOD_SPIDER = Eam.id("wood_spider");
	static final Identifier BLACK_WIDOW = Eam.id("black_widow");

	SpiderSettingsLoader() {
		super(ExtraCodecs.JSON, FileToIdConverter.json("mobs"));
	}

	@Override
	protected void apply(Map<Identifier, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
		SpiderModule.setSettings(read(files, WOOD_SPIDER, SpiderSettings.WOOD_SPIDER), read(files, BLACK_WIDOW, SpiderSettings.BLACK_WIDOW));
	}

	private static SpiderSettings read(Map<Identifier, JsonElement> files, Identifier id, SpiderSettings defaults) {
		JsonElement json = files.get(id);
		if (json == null) {
			return defaults;
		}
		return SpiderSettings.codec(defaults).parse(JsonOps.INSTANCE, json)
				.resultOrPartial(error -> Eam.LOGGER.warn("Bad {}: {}", id, error))
				.orElse(defaults);
	}
}
