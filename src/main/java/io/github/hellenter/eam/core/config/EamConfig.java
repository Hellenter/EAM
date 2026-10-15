package io.github.hellenter.eam.core.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
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
		boolean gear,
		boolean removeDiamondGear,
		boolean mobDrops,
		boolean ironPlates,
		boolean mobRunning,
		boolean defensiveCows,
		boolean blastFurnaceIron,
		boolean silver,
		boolean lockedVillages,
		boolean removeStructures,
		boolean anvilTiers,
		boolean slowCrops,
		boolean slowAnimals,
		boolean mithril,
		boolean anvilRepair,
		boolean safeSleep,
		boolean bloodMoon,
		boolean enchanting,
		boolean arrowHeads,
		boolean swampyForest,
		boolean talismanPortal,
		boolean arrowRecovery,
		boolean underworld,
		boolean livestockFeeding,
		boolean monsterEvolution,
		boolean blueMoon,
		boolean foodSpoilage
) {
	public static final EamConfig DEFAULT = new EamConfig(true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true,
			true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true);

	/**
	 * A codec group takes at most 16 fields, so flags after {@code iron_plates} go in this second map codec. It reads
	 * the same flat JSON object; add new flags here.
	 */
	private record LaterFlags(boolean mobRunning, boolean defensiveCows, boolean blastFurnaceIron, boolean silver,
			boolean lockedVillages, boolean removeStructures, boolean anvilTiers, boolean slowCrops, boolean slowAnimals, boolean mithril,
			boolean anvilRepair, boolean safeSleep, boolean bloodMoon,
			boolean enchanting, boolean arrowHeads, boolean swampyForest) {
		private static final MapCodec<LaterFlags> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
				Codec.BOOL.optionalFieldOf("mob_running", DEFAULT.mobRunning).forGetter(LaterFlags::mobRunning),
				Codec.BOOL.optionalFieldOf("defensive_cows", DEFAULT.defensiveCows).forGetter(LaterFlags::defensiveCows),
				Codec.BOOL.optionalFieldOf("blast_furnace_iron", DEFAULT.blastFurnaceIron).forGetter(LaterFlags::blastFurnaceIron),
				Codec.BOOL.optionalFieldOf("silver", DEFAULT.silver).forGetter(LaterFlags::silver),
				Codec.BOOL.optionalFieldOf("locked_villages", DEFAULT.lockedVillages).forGetter(LaterFlags::lockedVillages),
				Codec.BOOL.optionalFieldOf("remove_structures", DEFAULT.removeStructures).forGetter(LaterFlags::removeStructures),
				Codec.BOOL.optionalFieldOf("anvil_tiers", DEFAULT.anvilTiers).forGetter(LaterFlags::anvilTiers),
				Codec.BOOL.optionalFieldOf("slow_crops", DEFAULT.slowCrops).forGetter(LaterFlags::slowCrops),
				Codec.BOOL.optionalFieldOf("slow_animals", DEFAULT.slowAnimals).forGetter(LaterFlags::slowAnimals),
				Codec.BOOL.optionalFieldOf("mithril", DEFAULT.mithril).forGetter(LaterFlags::mithril),
				Codec.BOOL.optionalFieldOf("anvil_repair", DEFAULT.anvilRepair).forGetter(LaterFlags::anvilRepair),
				Codec.BOOL.optionalFieldOf("safe_sleep", DEFAULT.safeSleep).forGetter(LaterFlags::safeSleep),
				Codec.BOOL.optionalFieldOf("blood_moon", DEFAULT.bloodMoon).forGetter(LaterFlags::bloodMoon),
				Codec.BOOL.optionalFieldOf("enchanting", DEFAULT.enchanting).forGetter(LaterFlags::enchanting),
				Codec.BOOL.optionalFieldOf("arrow_heads", DEFAULT.arrowHeads).forGetter(LaterFlags::arrowHeads),
				Codec.BOOL.optionalFieldOf("swampy_forest", DEFAULT.swampyForest).forGetter(LaterFlags::swampyForest)
		).apply(instance, LaterFlags::new));

		private static LaterFlags of(EamConfig config) {
			return new LaterFlags(config.mobRunning, config.defensiveCows, config.blastFurnaceIron, config.silver, config.lockedVillages,
					config.removeStructures, config.anvilTiers, config.slowCrops, config.slowAnimals, config.mithril,
					config.anvilRepair, config.safeSleep, config.bloodMoon, config.enchanting, config.arrowHeads, config.swampyForest);
		}
	}

	/** The third map codec, for flags after {@code swampy_forest}: {@link LaterFlags} is full too. */
	private record LastFlags(boolean talismanPortal, boolean arrowRecovery, boolean underworld, boolean livestockFeeding,
			boolean monsterEvolution, boolean blueMoon, boolean foodSpoilage) {
		private static final MapCodec<LastFlags> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
				Codec.BOOL.optionalFieldOf("talisman_portal", DEFAULT.talismanPortal).forGetter(LastFlags::talismanPortal),
				Codec.BOOL.optionalFieldOf("arrow_recovery", DEFAULT.arrowRecovery).forGetter(LastFlags::arrowRecovery),
				Codec.BOOL.optionalFieldOf("underworld", DEFAULT.underworld).forGetter(LastFlags::underworld),
				Codec.BOOL.optionalFieldOf("livestock_feeding", DEFAULT.livestockFeeding).forGetter(LastFlags::livestockFeeding),
				Codec.BOOL.optionalFieldOf("monster_evolution", DEFAULT.monsterEvolution).forGetter(LastFlags::monsterEvolution),
				Codec.BOOL.optionalFieldOf("blue_moon", DEFAULT.blueMoon).forGetter(LastFlags::blueMoon),
				Codec.BOOL.optionalFieldOf("food_spoilage", DEFAULT.foodSpoilage).forGetter(LastFlags::foodSpoilage)
		).apply(instance, LastFlags::new));

		private static LastFlags of(EamConfig config) {
			return new LastFlags(config.talismanPortal, config.arrowRecovery, config.underworld, config.livestockFeeding,
					config.monsterEvolution, config.blueMoon, config.foodSpoilage);
		}
	}

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
			Codec.BOOL.optionalFieldOf("gear", DEFAULT.gear).forGetter(EamConfig::gear),
			Codec.BOOL.optionalFieldOf("remove_diamond_gear", DEFAULT.removeDiamondGear).forGetter(EamConfig::removeDiamondGear),
			Codec.BOOL.optionalFieldOf("mob_drops", DEFAULT.mobDrops).forGetter(EamConfig::mobDrops),
			Codec.BOOL.optionalFieldOf("iron_plates", DEFAULT.ironPlates).forGetter(EamConfig::ironPlates),
			LaterFlags.CODEC.forGetter(LaterFlags::of),
			LastFlags.CODEC.forGetter(LastFlags::of)
	).apply(instance, (hunger, thirst, progression, mining, stackSizes, mobs, mobBlockBreaking, removeEarlyTools, ores, crafting,
			gear, removeDiamondGear, mobDrops, ironPlates, later, last) -> new EamConfig(hunger, thirst, progression, mining, stackSizes, mobs,
			mobBlockBreaking, removeEarlyTools, ores, crafting, gear, removeDiamondGear, mobDrops, ironPlates, later.mobRunning,
			later.defensiveCows, later.blastFurnaceIron, later.silver, later.lockedVillages, later.removeStructures,
			later.anvilTiers, later.slowCrops, later.slowAnimals, later.mithril, later.anvilRepair,
			later.safeSleep, later.bloodMoon, later.enchanting, later.arrowHeads, later.swampyForest,
			last.talismanPortal, last.arrowRecovery, last.underworld, last.livestockFeeding, last.monsterEvolution, last.blueMoon,
			last.foodSpoilage)));

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
