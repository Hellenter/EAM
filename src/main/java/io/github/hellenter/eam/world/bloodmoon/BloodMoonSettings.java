package io.github.hellenter.eam.world.bloodmoon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;
import java.util.Map;

/**
 * Blood moon numbers, read from {@code data/eam/events/blood_moon.json} so datapacks can override them. A missing or
 * invalid field falls back to its default.
 *
 * @param firstMinDay        the first blood moon of a world comes on this day or later (day 1 is the first day)
 * @param firstMaxDay        ... and on this day at the latest
 * @param intervalMinDays    later blood moons come at least this many days after the previous one
 * @param intervalMaxDays    ... and at most this many
 * @param startTime          ticks into the day when the event begins (12000: sunset); it ends when the next day begins
 * @param damageMultiplier   damage hostile mobs deal during the event
 * @param senseRadius        hostile mobs that dig through blocks sense players through walls this far
 * @param dropMultiplier     a hostile mob's death loot is rolled this many times
 * @param experienceMultiplier experience from hostile mobs is multiplied by this
 * @param armorPieceWeights  odds of a mob wearing 1, 2, 3 or 4 armor pieces
 * @param armorMaterials     odds of each armor material; {@code minecraft:iron} means {@code minecraft:iron_chestplate}...
 * @param weaponChance       chance an armed mob holds a weapon
 * @param weaponMaterials    odds of each weapon material; {@code minecraft:iron} means {@code minecraft:iron_sword}...
 * @param weapons            odds of each weapon kind: the item name after the material
 */
public record BloodMoonSettings(int firstMinDay, int firstMaxDay, int intervalMinDays, int intervalMaxDays, int startTime,
		double damageMultiplier, int senseRadius, int dropMultiplier, double experienceMultiplier, List<Integer> armorPieceWeights,
		Map<String, Integer> armorMaterials, double weaponChance, Map<String, Integer> weaponMaterials, Map<String, Integer> weapons) {
	public static final BloodMoonSettings DEFAULT = new BloodMoonSettings(15, 20, 10, 20, 12_000, 1.2, 10, 2, 2.0,
			List.of(1, 1, 1, 1),
			Map.of("minecraft:chainmail", 1, "minecraft:copper", 1, "minecraft:iron", 1),
			0.7,
			Map.of("minecraft:copper", 1, "minecraft:iron", 1),
			Map.of("spear", 1, "sword", 1, "axe", 1));

	private static final Codec<Map<String, Integer>> WEIGHTS = Codec.unboundedMap(Codec.STRING, Codec.intRange(0, 10_000));

	public static final Codec<BloodMoonSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.intRange(1, 100_000).fieldOf("first_min_day").orElse(DEFAULT.firstMinDay).forGetter(BloodMoonSettings::firstMinDay),
			Codec.intRange(1, 100_000).fieldOf("first_max_day").orElse(DEFAULT.firstMaxDay).forGetter(BloodMoonSettings::firstMaxDay),
			Codec.intRange(1, 100_000).fieldOf("interval_min_days").orElse(DEFAULT.intervalMinDays).forGetter(BloodMoonSettings::intervalMinDays),
			Codec.intRange(1, 100_000).fieldOf("interval_max_days").orElse(DEFAULT.intervalMaxDays).forGetter(BloodMoonSettings::intervalMaxDays),
			Codec.intRange(0, BloodMoonLogic.DAY_LENGTH - 1).fieldOf("start_time").orElse(DEFAULT.startTime).forGetter(BloodMoonSettings::startTime),
			Codec.doubleRange(0.0, 100.0).fieldOf("damage_multiplier").orElse(DEFAULT.damageMultiplier).forGetter(BloodMoonSettings::damageMultiplier),
			Codec.intRange(0, 32).fieldOf("sense_radius").orElse(DEFAULT.senseRadius).forGetter(BloodMoonSettings::senseRadius),
			Codec.intRange(1, 16).fieldOf("drop_multiplier").orElse(DEFAULT.dropMultiplier).forGetter(BloodMoonSettings::dropMultiplier),
			Codec.doubleRange(0.0, 100.0).fieldOf("experience_multiplier").orElse(DEFAULT.experienceMultiplier).forGetter(BloodMoonSettings::experienceMultiplier),
			Codec.intRange(0, 10_000).listOf(4, 4).fieldOf("armor_piece_weights").orElse(DEFAULT.armorPieceWeights).forGetter(BloodMoonSettings::armorPieceWeights),
			WEIGHTS.fieldOf("armor_materials").orElse(DEFAULT.armorMaterials).forGetter(BloodMoonSettings::armorMaterials),
			Codec.doubleRange(0.0, 1.0).fieldOf("weapon_chance").orElse(DEFAULT.weaponChance).forGetter(BloodMoonSettings::weaponChance),
			WEIGHTS.fieldOf("weapon_materials").orElse(DEFAULT.weaponMaterials).forGetter(BloodMoonSettings::weaponMaterials),
			WEIGHTS.fieldOf("weapons").orElse(DEFAULT.weapons).forGetter(BloodMoonSettings::weapons)
	).apply(instance, BloodMoonSettings::new));
}
