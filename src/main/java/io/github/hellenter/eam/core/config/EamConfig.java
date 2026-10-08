package io.github.hellenter.eam.core.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.github.hellenter.eam.Eam;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Server-side config: which mechanics are enabled. Balance numbers live in data
 * (tags and datapack JSON), not here. Missing keys fall back to defaults, so old
 * config files keep working when new flags are added.
 */
public record EamConfig(
		boolean hunger,
		boolean thirst,
		boolean progression,
		boolean mining,
		boolean stackSizes,
		boolean mobs,
		boolean mobBlockBreaking,
		boolean removeEarlyTools,
		boolean ores,
		boolean crafting,
		boolean mobRunning,
		boolean defensiveCows
) {
	public static final EamConfig DEFAULT = new EamConfig(true, true, true, true, true, true, true, true, true, true, true, true);

	public static final Codec<EamConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.BOOL.optionalFieldOf("hunger", DEFAULT.hunger).forGetter(EamConfig::hunger),
			Codec.BOOL.optionalFieldOf("thirst", DEFAULT.thirst).forGetter(EamConfig::thirst),
			Codec.BOOL.optionalFieldOf("progression", DEFAULT.progression).forGetter(EamConfig::progression),
			Codec.BOOL.optionalFieldOf("mining", DEFAULT.mining).forGetter(EamConfig::mining),
			Codec.BOOL.optionalFieldOf("stack_sizes", DEFAULT.stackSizes).forGetter(EamConfig::stackSizes),
			Codec.BOOL.optionalFieldOf("mobs", DEFAULT.mobs).forGetter(EamConfig::mobs),
			Codec.BOOL.optionalFieldOf("mob_block_breaking", DEFAULT.mobBlockBreaking).forGetter(EamConfig::mobBlockBreaking),
			Codec.BOOL.optionalFieldOf("remove_early_tools", DEFAULT.removeEarlyTools).forGetter(EamConfig::removeEarlyTools),
			Codec.BOOL.optionalFieldOf("ores", DEFAULT.ores).forGetter(EamConfig::ores),
			Codec.BOOL.optionalFieldOf("crafting", DEFAULT.crafting).forGetter(EamConfig::crafting),
			Codec.BOOL.optionalFieldOf("mob_running", DEFAULT.mobRunning).forGetter(EamConfig::mobRunning),
			Codec.BOOL.optionalFieldOf("defensive_cows", DEFAULT.defensiveCows).forGetter(EamConfig::defensiveCows)
	).apply(instance, EamConfig::new));

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	public static EamConfig load() {
		Path path = FabricLoader.getInstance().getConfigDir().resolve(Eam.MOD_ID + ".json");
		EamConfig config = DEFAULT;

		if (Files.exists(path)) {
			try {
				JsonElement json = JsonParser.parseString(Files.readString(path));
				config = CODEC.parse(JsonOps.INSTANCE, json)
						.resultOrPartial(error -> Eam.LOGGER.error("Invalid {}: {}", path, error))
						.orElse(DEFAULT);
			} catch (Exception e) {
				Eam.LOGGER.error("Failed to read {}, using defaults", path, e);
			}
		}

		// Rewrite the file so newly added flags show up with their defaults.
		CODEC.encodeStart(JsonOps.INSTANCE, config).result().ifPresent(json -> {
			try {
				Files.writeString(path, GSON.toJson(json));
			} catch (IOException e) {
				Eam.LOGGER.error("Failed to write {}", path, e);
			}
		});

		return config;
	}
}
